package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.facturainterna.*;
import com.snnsoluciones.backnathbitpos.dto.orden.CrearOrdenRequest;
import com.snnsoluciones.backnathbitpos.dto.orden.OrdenResponse;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.*;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacturaInternaService {

    private final FacturaInternaRepository facturaInternaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;
    private final SesionCajaRepository sesionCajaRepository;
    private final OrdenService ordenService;
    private final MetricaProductoVendidoService metricaProductoService;
    private final PlataformaDigitalConfigRepository plataformaDigitalConfigRepository;

    /**
     * Crear una nueva factura interna
     */
    @Transactional
    public FacturaInternaResponse crear(CrearFacturaInternaRequest request) {
        log.info("Creando factura interna para empresa: {}", request.getEmpresaId());

        // ===== 1) Cargar entidades base =====
        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

        Usuario cajero = usuarioRepository.findById(request.getUsuarioId())
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        SesionCaja sesionCaja = sesionCajaRepository.findById(request.getSesionCajaId())
            .orElseThrow(() -> new ResourceNotFoundException("No hay sesion de caja abierta"));

        // ===== 2) Generar el número de factura ANTES (para usarlo en la Orden) =====
        final String numeroFactura = generarNumeroFactura(empresa.getId(), sucursal.getId());

        // ===== 3) Construir y crear ORDEN (ABRE ANTES DE LA FACTURA) =====
        // (Si es ventanilla, mesaId = null; si viene mesa desde el request, úsala)
        // Ajusta los getters al DTO real de tu request de factura interna:
        Long mesaId = null; // si tu DTO no lo trae, déjalo null
        Long clienteId = request.getClienteId();
        String nombreCliente = request.getNombreCliente();

        // Regla sencilla para servicio: 10% si mesa, 0% si ventanilla (ajústalo a tu negocio)
        BigDecimal pctServicio = BigDecimal.ZERO;

        // Mapear los detalles a items de orden
        List<CrearOrdenRequest.ItemRequest> itemsOrden = request.getDetalles().stream()
            .map(d -> new CrearOrdenRequest.ItemRequest(
                d.getProductoId(),
                d.getCantidad(),          // BigDecimal según tu DTO
                d.getNotas()              // observaciones/notas por línea
            ))
            .toList();

        CrearOrdenRequest crearOrdenRequest = new CrearOrdenRequest(
            mesaId,                       // Long mesaId
            sucursal.getId(),             // Long sucursalId
            clienteId,                    // Long clienteId
            nombreCliente,                // String nombreCliente
            1,                            // Integer numeroPersonas (MVP)
            pctServicio,                  // BigDecimal porcentajeServicio
            "Orden generada desde TIQ " + numeroFactura, // observaciones
            numeroFactura,                // String ordenNumero (¡aquí va el número de factura!)
            itemsOrden                    // List<ItemRequest> @NotEmpty
        );

        OrdenResponse orden = ordenService.crearOrden(crearOrdenRequest);
        log.info("Orden {} creada antes de factura interna {}", orden.numero(), numeroFactura);

        // ===== 4) Construir y guardar la FACTURA usando el MISMO número =====
        FacturaInterna factura = FacturaInterna.builder()
            .numero(numeroFactura)             // <<-- usar el ya generado
            .empresa(empresa)
            .sucursal(sucursal)
            .sesionCaja(sesionCaja)
            .cajero(cajero)
            .fecha(LocalDateTime.now())
            .estado("PAGADA")
            .notas(request.getNotas())
            .build();

        // Cliente opcional
        if (clienteId != null) {
            Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
            factura.setCliente(cliente);
            factura.setNombreCliente(cliente.getRazonSocial());
        } else if (nombreCliente != null) {
            factura.setNombreCliente(nombreCliente);
        }
        factura.setNumeroViper(request.getNumeroViper());


        // ===== 5) Detalles de la factura =====
        BigDecimal subtotal = BigDecimal.ZERO;

        for (DetalleFacturaInternaRequest detalleReq : request.getDetalles()) {
            Producto producto = productoRepository.findById(detalleReq.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + detalleReq.getProductoId()));

            FacturaInternaDetalle detalle = FacturaInternaDetalle.builder()
                .cantidad(detalleReq.getCantidad())
                .descuento(detalleReq.getDescuento() != null ? detalleReq.getDescuento() : BigDecimal.ZERO)
                .notas(detalleReq.getNotas())
                .build();

            detalle.setearDatosProducto(producto, detalleReq);

                // ✅ HOTFIX: Si viene subtotal en el request, usarlo directamente
            if (detalleReq.getSubtotal() != null) {
                detalle.setPrecioUnitario(detalleReq.getSubtotal().divide(detalleReq.getCantidad(), 2, RoundingMode.HALF_UP));
                detalle.setSubtotal(detalleReq.getSubtotal());
                detalle.setTotal(detalleReq.getSubtotal().subtract(detalle.getDescuento()));
            } else {
                detalle.calcularTotales(); // Fallback para compatibilidad
            }

            factura.agregarDetalle(detalle);
            subtotal = subtotal.add(detalle.getTotal());
        }

        // Totales
        factura.setSubtotal(subtotal);
        factura.setDescuento(request.getDescuento() != null ? request.getDescuento() : BigDecimal.ZERO);
        factura.setDescuentoPorcentaje(request.getDescuentoPorcentaje() != null ? request.getDescuentoPorcentaje() : BigDecimal.ZERO);
        factura.calcularTotal();

        // Medios de pago
        BigDecimal totalPagos = BigDecimal.ZERO;
        for (MedioPagoInternoRequest medioPagoReq : request.getMediosPago()) {
            FacturaInternaMedioPago medioPago = FacturaInternaMedioPago.builder()
                .tipo(medioPagoReq.getTipoPago())
                .monto(medioPagoReq.getMonto())
                .referencia(medioPagoReq.getReferencia())
                .banco(medioPagoReq.getBanco())
                .notas(medioPagoReq.getNumeroAutorizacion())
                .build();

            if (medioPagoReq.getPlataformaDigitalId() != null) {
                PlataformaDigitalConfig plataforma = plataformaDigitalConfigRepository
                    .findById(medioPagoReq.getPlataformaDigitalId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Plataforma digital no encontrada: " + medioPagoReq.getPlataformaDigitalId()));

                medioPago.setPlataformaDigital(plataforma);
            }

            factura.agregarMedioPago(medioPago);
            totalPagos = totalPagos.add(medioPagoReq.getMonto());
        }

        factura.setPagoRecibido(totalPagos);
        factura.calcularVuelto();

        // Guardar
        factura = facturaInternaRepository.save(factura);
        log.info("Factura interna creada: {}", factura.getNumero());
        metricaProductoService.actualizarDesdeFacturaInterna(factura);

        return mapToResponse(factura);
    }

    /**
     * Cambiar métodos de pago de una factura
     */
    @Transactional
    public void cambiarMetodosPago(Long facturaId, Long usuarioId, CambiarMetodosPagoRequest request) {
        log.info("🔄 Cambiando métodos de pago para factura ID: {}", facturaId);

        // Buscar factura
        FacturaInterna factura = facturaInternaRepository.findById(facturaId)
            .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada"));

        // Validaciones
        if ("ANULADA".equals(factura.getEstado())) {
            throw new BadRequestException("No se puede modificar una factura anulada");
        }

        // Calcular total de nuevos medios de pago
        BigDecimal totalNuevosPagos = request.getMediosPago().stream()
            .map(MedioPagoInternoRequest::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Validar que el total coincida con el total de la factura
        if (totalNuevosPagos.compareTo(factura.getTotal()) != 0) {
            throw new BadRequestException(
                String.format("El total de los medios de pago (₡%.2f) no coincide con el total de la factura (₡%.2f)",
                    totalNuevosPagos, factura.getTotal())
            );
        }

        // Eliminar medios de pago anteriores
        factura.getMediosPago().clear();

        // Agregar nuevos medios de pago
        for (MedioPagoInternoRequest medioPagoReq : request.getMediosPago()) {
            FacturaInternaMedioPago medioPago = FacturaInternaMedioPago.builder()
                .tipo(medioPagoReq.getTipoPago())
                .monto(medioPagoReq.getMonto())
                .referencia(medioPagoReq.getReferencia())
                .banco(medioPagoReq.getBanco())
                .notas(medioPagoReq.getNumeroAutorizacion())
                .build();

            // Si tiene plataforma digital
            if (medioPagoReq.getPlataformaDigitalId() != null) {
                PlataformaDigitalConfig plataforma = plataformaDigitalConfigRepository
                    .findById(medioPagoReq.getPlataformaDigitalId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Plataforma digital no encontrada: " + medioPagoReq.getPlataformaDigitalId()));
                medioPago.setPlataformaDigital(plataforma);
            }

            factura.agregarMedioPago(medioPago);
        }

        // Actualizar pago recibido
        factura.setPagoRecibido(totalNuevosPagos);
        factura.calcularVuelto();

        // Guardar
        facturaInternaRepository.save(factura);

        log.info("✅ Métodos de pago actualizados para factura {} por usuario {}. Motivo: {}",
            factura.getNumero(), usuarioId, request.getMotivo());
    }

    /**
     * Anular factura
     */
    @Transactional
    public void anular(Long facturaId, Long usuarioId, AnularFacturaRequest request) {
        FacturaInterna factura = facturaInternaRepository.findById(facturaId)
            .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada"));

        if ("ANULADA".equals(factura.getEstado())) {
            throw new BadRequestException("La factura ya está anulada");
        }

        factura.anular(usuarioId, request.getMotivo());

        facturaInternaRepository.save(factura);
        log.info("Factura {} anulada por usuario {}", factura.getNumero(), usuarioId);
    }

    /**
     * Buscar factura por ID
     */
    @Transactional(readOnly = true)
    public FacturaInternaResponse buscarPorId(Long id) {
        FacturaInterna factura = facturaInternaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada"));

        return mapToResponse(factura);
    }

    /**
     * Buscar facturas con filtros - VERSION FLEXIBLE
     */
    @Transactional(readOnly = true)
    public Page<FacturaInternaListResponse> buscar(
        Long empresaId,
        Long sucursalId,
        String estado,
        String fechaDesdeStr,
        String fechaHastaStr,
        String busqueda,
        Pageable pageable) {

        log.info("📋 Buscando facturas - empresa: {}, sucursal: {}, fechaDesde: {}, fechaHasta: {}, busqueda: '{}'",
            empresaId, sucursalId, fechaDesdeStr, fechaHastaStr, busqueda);

        // Crear Pageable con ordenamiento
        Pageable pageableOrdenado = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "fecha")
        );

        Page<FacturaInterna> facturas;

        // CASO 1: Búsqueda completa con fechas
        if (empresaId != null && sucursalId != null &&
            fechaDesdeStr != null && !fechaDesdeStr.isEmpty() &&
            fechaHastaStr != null && !fechaHastaStr.isEmpty()) {

            try {
                LocalDateTime fechaDesde = LocalDate.parse(fechaDesdeStr).atStartOfDay();
                LocalDateTime fechaHasta = LocalDate.parse(fechaHastaStr).atTime(23, 59, 59);

                if (busqueda != null && !busqueda.trim().isEmpty()) {
                    log.info("✅ Búsqueda CON filtro de texto");
                    facturas = facturaInternaRepository.buscarConFiltros(
                        empresaId, sucursalId, fechaDesde, fechaHasta, busqueda.trim(), pageableOrdenado);
                } else {
                    log.info("✅ Búsqueda por fechas solamente");
                    facturas = facturaInternaRepository.buscarPorFechas(
                        empresaId, sucursalId, fechaDesde, fechaHasta, pageableOrdenado);
                }
            } catch (Exception e) {
                log.error("❌ Error parseando fechas: {}", e.getMessage());
                // Fallback sin fechas
                facturas = facturaInternaRepository.findBySucursalId(sucursalId, pageableOrdenado);
            }
        }
        // CASO 2: Solo sucursal (sin fechas)
        else if (sucursalId != null) {
            log.info("✅ Búsqueda solo por sucursal");
            facturas = facturaInternaRepository.findBySucursalId(sucursalId, pageableOrdenado);
        }
        // CASO 3: Empresa + Estado
        else if (empresaId != null && estado != null) {
            log.info("✅ Búsqueda por empresa y estado");
            facturas = facturaInternaRepository.findByEmpresaIdAndEstado(empresaId, estado, pageableOrdenado);
        }
        // CASO 4: Solo empresa
        else if (empresaId != null) {
            log.info("✅ Búsqueda solo por empresa");
            facturas = facturaInternaRepository.findByEmpresaId(empresaId, pageableOrdenado);
        }
        // CASO 5: Sin filtros (todas)
        else {
            log.info("✅ Listando todas las facturas");
            facturas = facturaInternaRepository.findAll(pageableOrdenado);
        }

        log.info("📊 Resultados encontrados: {}", facturas.getTotalElements());
        return facturas.map(this::mapToListResponse);
    }

    /**
     * Generar número de factura
     */
    private String generarNumeroFactura(Long empresaId, Long sucursalId) {
        String prefix = sucursalId.toString() + LocalDate.now().getYear() + "-";

        List<String> ultimos = facturaInternaRepository.findUltimoNumeroByEmpresaAndPrefix(
            empresaId, prefix, Pageable.ofSize(1)
        );

        int siguiente = 1;
        if (!ultimos.isEmpty()) {
            String ultimo = ultimos.get(0);
            String numeroStr = ultimo.replace(prefix, "");
            siguiente = Integer.parseInt(numeroStr) + 1;
        }

        return prefix + String.format("%05d", siguiente);
    }

    /**
     * Mapear a response completo
     */
    private FacturaInternaResponse mapToResponse(FacturaInterna factura) {
        return FacturaInternaResponse.builder()
            .id(factura.getId())
            .numero(factura.getNumero())
            .fecha(factura.getFecha())
            .empresaNombre(factura.getEmpresa().getNombreRazonSocial())
            .sucursalNombre(factura.getSucursal().getNombre())
            .cajeroNombre(factura.getCajero().getNombre())
            .clienteId(factura.getCliente() != null ? factura.getCliente().getId() : null)
            .clienteNombre(factura.getNombreCliente())
            .subtotal(factura.getSubtotal())
            .descuentoPorcentaje(factura.getDescuentoPorcentaje())
            .descuento(factura.getDescuento())
            .total(factura.getTotal())
            .pagoRecibido(factura.getPagoRecibido())
            .vuelto(factura.getVuelto())
            .estado(factura.getEstado())
            .notas(factura.getNotas())
            .detalles(factura.getDetalles().stream()
                .map(this::mapDetalleToResponse)
                .collect(Collectors.toList()))
            .mediosPago(factura.getMediosPago().stream()
                .map(this::mapMedioPagoToResponse)
                .collect(Collectors.toList()))
            .build();
    }

    /**
     * Mapear a response de lista
     */
    private FacturaInternaListResponse mapToListResponse(FacturaInterna factura) {
        return FacturaInternaListResponse.builder()
            .id(factura.getId())
            .numero(factura.getNumero())
            .fecha(factura.getFecha())
            .clienteNombre(factura.getNombreCliente())
            .total(factura.getTotal())
            .estado(factura.getEstado())
            .build();
    }

    /**
     * Mapear detalle a response
     */
    private DetalleFacturaInternaResponse mapDetalleToResponse(FacturaInternaDetalle detalle) {
        return DetalleFacturaInternaResponse.builder()
            .id(detalle.getId())
            .productoId(detalle.getProducto().getId())
            .codigoProducto(detalle.getCodigoProducto())
            .nombreProducto(detalle.getNombreProducto())
            .cantidad(detalle.getCantidad())
            .precioUnitario(detalle.getPrecioUnitario())
            .subtotal(detalle.getSubtotal())
            .descuento(detalle.getDescuento())
            .total(detalle.getTotal())
            .notas(detalle.getNotas())
            .build();
    }

    /**
     * Mapear medio pago a response
     */
    private MedioPagoResponse mapMedioPagoToResponse(FacturaInternaMedioPago medioPago) {
        return MedioPagoResponse.builder()
            .tipoPago(medioPago.getTipo())
            .monto(medioPago.getMonto())
            .referencia(medioPago.getReferencia())
            .banco(medioPago.getBanco())
            .numeroAutorizacion(medioPago.getNotas())
            .build();
    }
}