package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.facturainterna.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Crear una nueva factura interna
     */
    @Transactional
    public FacturaInternaResponse crear(CrearFacturaInternaRequest request) {
        log.info("Creando factura interna para empresa: {}", request.getEmpresaId());

        // Obtener entidades desde el request
        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

        Usuario cajero = usuarioRepository.findById(request.getUsuarioId())
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Crear factura
        FacturaInterna factura = FacturaInterna.builder()
            .numero(generarNumeroFactura(request.getEmpresaId()))
            .empresa(empresa)
            .sucursal(sucursal)
            .cajero(cajero)
            .fecha(LocalDateTime.now())
            .estado("PAGADA")
            .notas(request.getNotas())
            .build();

        // Cliente opcional
        if (request.getClienteId() != null) {
            Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
            factura.setCliente(cliente);
            factura.setNombreCliente(cliente.getRazonSocial());
        } else if (request.getNombreCliente() != null) {
            factura.setNombreCliente(request.getNombreCliente());
        }

        // Procesar detalles
        BigDecimal subtotal = BigDecimal.ZERO;

        for (DetalleFacturaInternaRequest detalleReq : request.getDetalles()) {
            Producto producto = productoRepository.findById(detalleReq.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + detalleReq.getProductoId()));

            FacturaInternaDetalle detalle = FacturaInternaDetalle.builder()
                .cantidad(detalleReq.getCantidad())
                .descuento(detalleReq.getDescuento() != null ? detalleReq.getDescuento() : BigDecimal.ZERO)
                .notas(detalleReq.getNotas())
                .build();

            detalle.setearDatosProducto(producto);
            detalle.calcularTotales();

            factura.agregarDetalle(detalle);
            subtotal = subtotal.add(detalle.getTotal());
        }

        // Calcular totales
        factura.setSubtotal(subtotal);
        factura.setDescuento(request.getDescuento() != null ? request.getDescuento() : BigDecimal.ZERO);
        factura.calcularTotal();

        // Procesar medios de pago
        BigDecimal totalPagos = BigDecimal.ZERO;

        for (MedioPagoInternoRequest medioPagoReq : request.getMediosPago()) {
            FacturaInternaMedioPago medioPago = FacturaInternaMedioPago.builder()
                .tipo(medioPagoReq.getTipo())
                .monto(medioPagoReq.getMonto())
                .referencia(medioPagoReq.getReferencia())
                .banco(medioPagoReq.getBanco())
                .notas(medioPagoReq.getNotas())
                .build();

            factura.agregarMedioPago(medioPago);
            totalPagos = totalPagos.add(medioPagoReq.getMonto());
        }

        factura.setPagoRecibido(totalPagos);
        factura.calcularVuelto();

        // Guardar
        factura = facturaInternaRepository.save(factura);
        log.info("Factura interna creada: {}", factura.getNumero());

        return mapToResponse(factura);
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
     * Buscar facturas con filtros
     */
    @Transactional(readOnly = true)
    public Page<FacturaInternaListResponse> buscar(Long empresaId, Long sucursalId, String estado, Pageable pageable) {
        Page<FacturaInterna> facturas;

        if (sucursalId != null) {
            facturas = facturaInternaRepository.findBySucursalId(sucursalId, pageable);
        } else if (empresaId != null && estado != null) {
            facturas = facturaInternaRepository.findByEmpresaIdAndEstado(empresaId, estado, pageable);
        } else if (empresaId != null) {
            facturas = facturaInternaRepository.findByEmpresaId(empresaId, pageable);
        } else {
            facturas = facturaInternaRepository.findAll(pageable);
        }

        return facturas.map(this::mapToListResponse);
    }

    /**
     * Obtener siguiente número de factura
     */
    public String obtenerSiguienteNumero(Long empresaId) {
        return generarNumeroFactura(empresaId);
    }

    /**
     * Generar número de factura
     */
    private String generarNumeroFactura(Long empresaId) {
        String prefix = "INT-" + LocalDate.now().getYear() + "-";

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