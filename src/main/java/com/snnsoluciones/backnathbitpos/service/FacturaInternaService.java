package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.factura.FacturaResponse.MedioPagoDto;
import com.snnsoluciones.backnathbitpos.dto.facturainterna.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.mh.MedioPago;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.repository.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacturaInternaService {
    
    private final FacturaInternaRepository facturaRepository;
    private final FacturaInternaDetalleRepository detalleRepository;
    private final FacturaInternaMediosPagoRepository mediosPagoRepository;
    private final FacturaInternaOtrosCargosRepository otrosCargosRepository;
    private final FacturaInternaDescuentosRepository descuentosRepository;
    private final FacturaInternaBitacoraRepository bitacoraRepository;
    private final EmpresaService empresaService;
    private final SucursalService sucursalService;
    private final ClienteService clienteService;
    
    private final ProductoRepository productoRepository;
    private final UsuarioService usuarioService;
    
    @Transactional
    public FacturaInternaResponse crear(FacturaInternaRequest request) {
        log.info("Creando factura interna para sucursal: {}", request.getSucursalId());
        
        // Crear factura
        FacturaInterna factura = new FacturaInterna();
        factura.setEmpresa(empresaService.buscarPorId(request.getEmpresaId()));
        factura.setSucursal(sucursalService.finById(request.getSucursalId()).orElse(null));
        factura.setCliente(clienteService.obtenerPorId(request.getClienteId()));
        factura.setNombreCliente(request.getNombreCliente());
        factura.setUsuario(usuarioService.buscarPorId(request.getClienteId()).orElse(null));
        factura.setFechaEmision(LocalDateTime.now());
        factura.setNotas(request.getNotas());
        
        // Generar número de factura
        factura.setNumeroFactura(generarNumeroFactura(request.getSucursalId()));
        
        // Calcular totales
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDescuentos = BigDecimal.ZERO;
        BigDecimal totalOtrosCargos = BigDecimal.ZERO;
        
        // Guardar factura primero
        factura = facturaRepository.save(factura);
        
        // Procesar detalles
        int numeroLinea = 1;
        for (FacturaInternaRequest.DetalleRequest detalleReq : request.getDetalles()) {
            FacturaInternaDetalle detalle = procesarDetalle(factura, detalleReq, numeroLinea++);
            subtotal = subtotal.add(detalle.getSubtotal());
            totalOtrosCargos = totalOtrosCargos.add(detalle.getMontoImpuestoServicio());
        }
        
        // Procesar descuentos
        if (request.getDescuentos() != null) {
            for (FacturaInternaRequest.DescuentoRequest descuentoReq : request.getDescuentos()) {
                FacturaInternaDescuentos descuento = procesarDescuento(factura, descuentoReq);
                totalDescuentos = totalDescuentos.add(descuento.getMonto());
            }
        }
        
        // Procesar otros cargos
        if (request.getOtrosCargos() != null) {
            for (FacturaInternaRequest.OtroCargoRequest cargoReq : request.getOtrosCargos()) {
                FacturaInternaOtrosCargos cargo = procesarOtroCargo(factura, cargoReq);
                totalOtrosCargos = totalOtrosCargos.add(cargo.getMonto());
            }
        }
        
        // Actualizar totales
        factura.setSubtotal(subtotal);
        factura.setTotalDescuentos(totalDescuentos);
        factura.setTotalOtrosCargos(totalOtrosCargos);
        factura.setTotalVenta(subtotal.subtract(totalDescuentos).add(totalOtrosCargos));
        
        // Procesar pagos
        procesarPagos(factura, request.getMediosPago());
        
        // Guardar actualización
        factura = facturaRepository.save(factura);
        
        // Registrar en bitácora
        registrarBitacora(factura, "CREADA", "Factura interna creada");
        
        return mapToResponse(factura);
    }
    
    private FacturaInternaDetalle procesarDetalle(FacturaInterna factura, 
                                                 FacturaInternaRequest.DetalleRequest request,
                                                 int numeroLinea) {
        Producto producto = productoRepository.findById(request.getProductoId())
            .orElseThrow(() -> new BusinessException("Producto no encontrado"));
        
        FacturaInternaDetalle detalle = new FacturaInternaDetalle();
        detalle.setFactura(factura);
        detalle.setNumeroLinea(numeroLinea);
        detalle.setProducto(producto);
        detalle.setCodigoProducto(producto.getCodigoInterno());
        detalle.setDescripcion(producto.getNombre());
        detalle.setCantidad(request.getCantidad());
        detalle.setPrecioUnitario(request.getPrecioUnitario());
        detalle.setPorcentajeDescuento(request.getPorcentajeDescuento());
        detalle.setMontoImpuestoServicio(request.getMontoImpuestoServicio());
        detalle.setNotas(request.getNotas());
        
        // Calcular montos
        BigDecimal montoLinea = request.getCantidad().multiply(request.getPrecioUnitario());
        BigDecimal montoDescuento = montoLinea.multiply(request.getPorcentajeDescuento())
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        
        detalle.setMontoDescuento(montoDescuento);
        detalle.setSubtotal(montoLinea.subtract(montoDescuento));
        detalle.setMontoTotalLinea(detalle.getSubtotal().add(request.getMontoImpuestoServicio()));
        
        return detalleRepository.save(detalle);
    }
    
    private void procesarPagos(FacturaInterna factura, List<FacturaInternaRequest.MedioPagoRequest> pagos) {
        if (pagos == null || pagos.isEmpty()) {
            throw new BusinessException("Debe especificar al menos un medio de pago");
        }
        
        BigDecimal totalPagado = BigDecimal.ZERO;
        
        for (FacturaInternaRequest.MedioPagoRequest pagoReq : pagos) {
            FacturaInternaMediosPago pago = new FacturaInternaMediosPago();
            pago.setFactura(factura);
            pago.setTipoPago(MedioPago.valueOf(pagoReq.getTipoPago()));
            pago.setMonto(pagoReq.getMonto());
            pago.setReferencia(pagoReq.getReferencia());
            pago.setBanco(pagoReq.getBanco());
            pago.setNumeroAutorizacion(pagoReq.getNumeroAutorizacion());
            
            mediosPagoRepository.save(pago);
            totalPagado = totalPagado.add(pagoReq.getMonto());
        }
        
        // Calcular vuelto si es efectivo
        if (totalPagado.compareTo(factura.getTotalVenta()) > 0) {
            BigDecimal vuelto = totalPagado.subtract(factura.getTotalVenta());
            // Actualizar el pago en efectivo con el vuelto
            mediosPagoRepository.findByFacturaIdAndTipoPago(factura.getId(), MedioPago.EFECTIVO)
                .stream().findFirst().ifPresent(pago -> {
                    pago.setCambio(vuelto);
                    mediosPagoRepository.save(pago);
                });
        }
    }
    
    private String generarNumeroFactura(Long sucursalId) {
        String year = String.valueOf(Year.now().getValue());
        Integer ultimoNumero = facturaRepository.findMaxNumeroFactura(sucursalId, year)
            .orElse(0);
        
        return String.format("FI-%s-%05d", year, ultimoNumero + 1);
    }
    
    private void registrarBitacora(FacturaInterna factura, String accion, String descripcion) {
        FacturaInternaBitacora bitacora = new FacturaInternaBitacora();
        bitacora.setFactura(factura);
        bitacora.setAccion(accion);
        bitacora.setUsuario((factura.getUsuario()));
        bitacora.setDescripcion(descripcion);
        bitacora.setFechaAccion(LocalDateTime.now());
        
        bitacoraRepository.save(bitacora);
    }
    
    @Transactional
    public void anular(Long facturaId, String motivo) {
        FacturaInterna factura = facturaRepository.findById(facturaId)
            .orElseThrow(() -> new BusinessException("Factura no encontrada"));
        
        if ("ANULADA".equals(factura.getEstado())) {
            throw new BusinessException("La factura ya está anulada");
        }
        
        factura.setEstado("ANULADA");
        factura.setAnuladaPor(factura.getUsuario().getId());
        factura.setFechaAnulacion(LocalDateTime.now());
        factura.setMotivoAnulacion(motivo);
        
        facturaRepository.save(factura);
        
        registrarBitacora(factura, "ANULADA", "Factura anulada: " + motivo);
    }
    
    // Métodos auxiliares omitidos por brevedad...

    // Continuación de FacturaInternaService.java

    // ========== MÉTODOS AUXILIARES ==========

    private FacturaInternaDescuentos procesarDescuento(FacturaInterna factura,
        FacturaInternaRequest.DescuentoRequest request) {
        FacturaInternaDescuentos descuento = new FacturaInternaDescuentos();
        descuento.setFactura(factura);
        descuento.setTipoDescuento(request.getTipoDescuento());
        descuento.setDescripcion(request.getDescripcion());
        descuento.setPorcentaje(request.getPorcentaje());
        descuento.setCodigoPromocion(request.getCodigoPromocion());

        // Calcular monto si viene por porcentaje
        if (request.getMonto() != null) {
            descuento.setMonto(request.getMonto());
        } else if (request.getPorcentaje() != null) {
            BigDecimal montoDescuento = factura.getSubtotal()
                .multiply(request.getPorcentaje())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            descuento.setMonto(montoDescuento);
        }

        return descuentosRepository.save(descuento);
    }

    private FacturaInternaOtrosCargos procesarOtroCargo(FacturaInterna factura,
        FacturaInternaRequest.OtroCargoRequest request) {
        FacturaInternaOtrosCargos cargo = new FacturaInternaOtrosCargos();
        cargo.setFactura(factura);
        cargo.setTipoCargo(request.getTipoCargo());
        cargo.setDescripcion(request.getDescripcion());
        cargo.setPorcentaje(request.getPorcentaje());
        cargo.setAplicadoAutomaticamente(request.getAplicadoAutomaticamente());

        // Calcular monto si viene por porcentaje
        if (request.getMonto() != null) {
            cargo.setMonto(request.getMonto());
        } else if (request.getPorcentaje() != null) {
            BigDecimal montoCargo = factura.getSubtotal()
                .multiply(request.getPorcentaje())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            cargo.setMonto(montoCargo);
        }

        return otrosCargosRepository.save(cargo);
    }

    private FacturaInternaResponse mapToResponse(FacturaInterna factura) {
        return FacturaInternaResponse.builder()
            .id(factura.getId())
            .numeroFactura(factura.getNumeroFactura())
            .estado(factura.getEstado())
            .fechaEmision(factura.getFechaEmision())
            .clienteId(factura.getCliente().getId())
            .nombreCliente(factura.getNombreCliente())
            .subtotal(factura.getSubtotal())
            .totalDescuentos(factura.getTotalDescuentos())
            .totalOtrosCargos(factura.getTotalOtrosCargos())
            .totalVenta(factura.getTotalVenta())
            .empresaNombre(factura.getEmpresa().getNombreComercial())
            .sucursalNombre(factura.getSucursal().getNombre())
            .cajeroNombre(factura.getUsuario().getNombre())
            .notas(factura.getNotas())
            .detalles(mapDetalles(factura.getId()))
            .mediosPago(mapMediosPago(factura.getId()))
            .otrosCargos(mapOtrosCargos(factura.getId()))
            .descuentos(mapDescuentos(factura.getId()))
            .build();
    }

    private List<FacturaInternaResponse.DetalleResponse> mapDetalles(Long facturaId) {
        return detalleRepository.findByFacturaIdOrderByNumeroLinea(facturaId)
            .stream()
            .map(detalle -> FacturaInternaResponse.DetalleResponse.builder()
                .id(detalle.getId())
                .numeroLinea(detalle.getNumeroLinea())
                .codigoProducto(detalle.getCodigoProducto())
                .descripcion(detalle.getDescripcion())
                .cantidad(detalle.getCantidad())
                .unidadMedida(detalle.getUnidadMedida())
                .precioUnitario(detalle.getPrecioUnitario())
                .montoDescuento(detalle.getMontoDescuento())
                .subtotal(detalle.getSubtotal())
                .montoImpuestoServicio(detalle.getMontoImpuestoServicio())
                .montoTotalLinea(detalle.getMontoTotalLinea())
                .build())
            .collect(Collectors.toList());
    }

    private List<MedioPagoResponse> mapMediosPago(Long facturaId) {
        return mediosPagoRepository.findByFacturaId(facturaId)
            .stream()
            .map(pago -> MedioPagoResponse.builder()
                .id(pago.getId())
                .tipoPago(pago.getTipoPago().name())
                .descripcionPago(pago.getTipoPago().getDescripcion())
                .monto(pago.getMonto())
                .referencia(pago.getReferencia())
                .cambio(pago.getCambio())
                .banco(pago.getBanco())
                .numeroAutorizacion(pago.getNumeroAutorizacion())
                .build())
            .collect(Collectors.toList());
    }

    private List<OtroCargoResponse> mapOtrosCargos(Long facturaId) {
        return otrosCargosRepository.findByFacturaId(facturaId)
            .stream()
            .map(cargo -> OtroCargoResponse.builder()
                .id(cargo.getId())
                .tipoCargo(cargo.getTipoCargo())
                .descripcion(cargo.getDescripcion())
                .porcentaje(cargo.getPorcentaje())
                .monto(cargo.getMonto())
                .aplicadoAutomaticamente(cargo.getAplicadoAutomaticamente())
                .build())
            .collect(Collectors.toList());
    }

    private List<DescuentoResponse> mapDescuentos(Long facturaId) {
        return descuentosRepository.findByFacturaId(facturaId)
            .stream()
            .map(descuento -> DescuentoResponse.builder()
                .id(descuento.getId())
                .tipoDescuento(descuento.getTipoDescuento())
                .descripcion(descuento.getDescripcion())
                .porcentaje(descuento.getPorcentaje())
                .monto(descuento.getMonto())
                .codigoPromocion(descuento.getCodigoPromocion())
                .build())
            .collect(Collectors.toList());
    }

    // ========== MÉTODOS DE CONSULTA ==========

    @Transactional(readOnly = true)
    public FacturaInternaResponse obtenerPorId(Long id) {
        FacturaInterna factura = facturaRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Factura no encontrada"));

        return mapToResponse(factura);
    }

    @Transactional(readOnly = true)
    public List<FacturaInternaResponse> obtenerFacturasHoy(Long sucursalId) {
        return facturaRepository.findFacturasHoy(sucursalId)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<FacturaInternaResponse> buscar(FacturaInternaSearchRequest request, Pageable pageable) {
        Page<FacturaInterna> facturas = facturaRepository.buscarConFiltros(
            request.getEmpresaId(),
            request.getSucursalId(),
            request.getEstado(),
            request.getNumeroFactura(),
            request.getFechaInicio(),
            request.getFechaFin(),
            pageable
        );

        return facturas.map(this::mapToResponse);
    }

    // ========== VALIDACIONES ==========

    private void validarTotales(FacturaInterna factura, List<FacturaInternaRequest.MedioPagoRequest> pagos) {
        BigDecimal totalPagos = pagos.stream()
            .map(FacturaInternaRequest.MedioPagoRequest::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPagos.compareTo(factura.getTotalVenta()) < 0) {
            throw new BusinessException("El total de pagos es menor al total de la factura");
        }
    }

    private void validarEstadoFactura(FacturaInterna factura, String operacion) {
        if ("ANULADA".equals(factura.getEstado())) {
            throw new BusinessException("No se puede " + operacion + " una factura anulada");
        }
    }

    // ========== MÉTODOS DE REPORTE ==========

    @Transactional(readOnly = true)
    public ResumenDiarioResponse obtenerResumenDiario(Long sucursalId, LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(23, 59, 59);

        List<FacturaInterna> facturas = facturaRepository.findByFechaEmisionBetween(inicio, fin, Pageable.unpaged())
            .stream()
            .filter(f -> f.getSucursal().getId().equals(sucursalId))
            .toList();

        BigDecimal totalVentas = facturas.stream()
            .filter(f -> !"ANULADA".equals(f.getEstado()))
            .map(FacturaInterna::getTotalVenta)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long cantidadFacturas = facturas.stream()
            .filter(f -> !"ANULADA".equals(f.getEstado()))
            .count();

        long cantidadAnuladas = facturas.stream()
            .filter(f -> "ANULADA".equals(f.getEstado()))
            .count();

        // Agrupar por medio de pago
        Map<String, BigDecimal> totalesPorMedioPago = new HashMap<>();
        for (FacturaInterna factura : facturas) {
            if (!"ANULADA".equals(factura.getEstado())) {
                List<FacturaInternaMediosPago> pagos = mediosPagoRepository.findByFacturaId(factura.getId());
                for (FacturaInternaMediosPago pago : pagos) {
                    String tipoPago = pago.getTipoPago().getDescripcion();
                    totalesPorMedioPago.merge(tipoPago, pago.getMonto(), BigDecimal::add);
                }
            }
        }

        return ResumenDiarioResponse.builder()
            .fecha(fecha)
            .sucursalId(sucursalId)
            .cantidadFacturas(cantidadFacturas)
            .cantidadAnuladas(cantidadAnuladas)
            .totalVentas(totalVentas)
            .totalesPorMedioPago(totalesPorMedioPago)
            .build();
    }

    // ========== MÉTODOS DE UTILIDAD ==========

    public boolean existeNumeroFactura(String numeroFactura) {
        return facturaRepository.findByNumeroFactura(numeroFactura).isPresent();
    }

    @Transactional
    public void actualizarNotas(Long facturaId, String notas) {
        FacturaInterna factura = facturaRepository.findById(facturaId)
            .orElseThrow(() -> new BusinessException("Factura no encontrada"));

        validarEstadoFactura(factura, "actualizar");

        String notasAnteriores = factura.getNotas();
        factura.setNotas(notas);
        factura.setUpdatedAt(LocalDateTime.now());

        facturaRepository.save(factura);

        registrarBitacora(factura, "MODIFICADA",
            "Notas actualizadas. Anterior: " + notasAnteriores + ", Nueva: " + notas);
    }

    @Transactional(readOnly = true)
    public byte[] generarPDF(Long facturaId) {
        FacturaInterna factura = facturaRepository.findById(facturaId)
            .orElseThrow(() -> new BusinessException("Factura no encontrada"));

        // Aquí iría la lógica de generación de PDF
        // Por ahora retornamos un array vacío
        log.info("Generando PDF para factura: {}", factura.getNumeroFactura());

        return new byte[0]; // TODO: Implementar generación de PDF
    }
}