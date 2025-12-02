package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.facturainterna.CrearFacturaInternaRequest;
import com.snnsoluciones.backnathbitpos.dto.facturainterna.DetalleFacturaInternaRequest;
import com.snnsoluciones.backnathbitpos.dto.facturainterna.MedioPagoInternoRequest;
import com.snnsoluciones.backnathbitpos.dto.orden.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.EstadoMesa;
import com.snnsoluciones.backnathbitpos.enums.EstadoOrden;
import com.snnsoluciones.backnathbitpos.enums.EstadoPagoItem;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.security.ContextoUsuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio para manejar pagos parciales de órdenes
 * Permite que múltiples personas paguen su parte de una orden
 * sin cerrar la orden hasta que todos hayan pagado
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PagoParcialService {

    private final OrdenRepository ordenRepository;
    private final OrdenItemRepository ordenItemRepository;
    private final MesaRepository mesaRepository;
    private final SesionCajaRepository sesionCajaRepository;
    private final FacturaInternaService facturaInternaService;
    private final FacturaInternaRepository facturaInternaRepository;

    // =============================================
    // MÉTODO PRINCIPAL: PROCESAR PAGO PARCIAL
    // =============================================

    @Transactional
    public MarcarItemsPagadosResponse marcarItemsPagados(Long ordenId, MarcarItemsPagadosRequest request) {
        log.info("📝 Marcando items como pagados - Orden: {}, Items: {}", ordenId, request.getItemIds());

        // 1. Buscar orden
        Orden orden = ordenRepository.findById(ordenId)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

        // 2. Buscar factura interna (si viene)
        FacturaInterna facturaInterna = null;
        if (request.getFacturaInternaId() != null) {
            facturaInterna = facturaInternaRepository.findById(request.getFacturaInternaId())
                .orElse(null);
        }

        // 3. Marcar items como pagados
        for (OrdenItem item : orden.getItems()) {
            if (request.getItemIds().contains(item.getId())) {
                item.setEstadoPago(EstadoPagoItem.PAGADO);
                item.setFechaPago(LocalDateTime.now());
                if (facturaInterna != null) {
                    item.setFacturaInterna(facturaInterna);
                }
            }
        }

        // 4. Verificar si todos los items están pagados
        boolean todosPageados = orden.getItems().stream()
            .allMatch(item -> item.getEstadoPago() == EstadoPagoItem.PAGADO);

        boolean mesaLiberada = false;

        if (todosPageados) {
            orden.setEstado(EstadoOrden.PAGADA);
            orden.setFechaCierre(LocalDateTime.now());

            // Liberar mesa
            if (orden.getMesa() != null) {
                orden.getMesa().setEstado(EstadoMesa.DISPONIBLE);
                mesaRepository.save(orden.getMesa());
                mesaLiberada = true;
            }
        }

        // 5. Guardar
        ordenRepository.save(orden);

        // 6. Contar pendientes
        int pendientes = (int) orden.getItems().stream()
            .filter(item -> item.getEstadoPago() != EstadoPagoItem.PAGADO)
            .count();

        log.info("✅ Items marcados - Orden cerrada: {}, Pendientes: {}, Mesa liberada: {}",
            todosPageados, pendientes, mesaLiberada);

        return MarcarItemsPagadosResponse.builder()
            .ordenCerrada(todosPageados)
            .itemsPendientes(pendientes)
            .mesaLiberada(mesaLiberada)
            .build();
    }

    @Transactional
    public PagoParcialResponse procesarPagoParcial(Long ordenId, PagoParcialRequest request) {
        log.info("📝 Procesando pago parcial para orden ID: {} - Items: {}",
            ordenId, request.getItemIds());

        // ===== 1. OBTENER CONTEXTO DE USUARIO =====
        ContextoUsuario contexto = (ContextoUsuario) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
        Long usuarioId = contexto.getUserId();

        // Obtener sesión de caja activa
        SesionCaja sesionCaja = sesionCajaRepository.findSesionActivaByUsuarioId(usuarioId)
            .orElseThrow(() -> new BusinessException("No hay sesión de caja activa para el usuario"));

        // ===== 2. VALIDACIONES =====
        Orden orden = validarOrdenParaPago(ordenId);
        List<OrdenItem> itemsAPagar = validarItemsParaPago(orden, request.getItemIds());
        validarMediosPago(request);

        // ===== 3. CALCULAR TOTAL DE ITEMS SELECCIONADOS =====
        BigDecimal totalItemsAPagar = calcularTotalItems(itemsAPagar);
        log.info("💰 Total items a pagar: {}", totalItemsAPagar);

        // ===== 4. GENERAR FACTURA SEGÚN TIPO =====
        PagoParcialResponse.PagoParcialResponseBuilder responseBuilder = PagoParcialResponse.builder();

        String tipoDoc = request.getTipoDocumento().toUpperCase();

        switch (tipoDoc) {
            case "TI", "FI" -> {
                // Factura Interna
                FacturaInterna facturaInterna = generarFacturaInterna(
                    orden, itemsAPagar, request, usuarioId, sesionCaja.getId());

                responseBuilder
                    .tipoDocumentoGenerado(tipoDoc)
                    .facturaInternaId(facturaInterna.getId())
                    .numeroInterno(facturaInterna.getNumero())
                    .totalFacturado(facturaInterna.getTotal())
                    .vuelto(facturaInterna.getVuelto());

                // Marcar items como pagados
                marcarItemsPagadosConFacturaInterna(itemsAPagar, facturaInterna);

                // Registrar factura en la orden
                orden.agregarFacturaInternaParcial(facturaInterna);
            }
            case "TE", "FE" -> {
                throw new BusinessException(
                    "Documentos electrónicos para pago parcial aún no implementados. " +
                        "Use TI (Tiquete Interno) o FI (Factura Interna)."
                );
            }
            default -> throw new BadRequestException(
                "Tipo de documento no válido: " + tipoDoc + ". Use: TI, FI, TE o FE"
            );
        }

        // ===== 5. ACTUALIZAR ESTADO DE LA ORDEN =====
        boolean ordenCerrada = actualizarEstadoOrden(orden);

        // ===== 6. LIBERAR MESA SI CORRESPONDE =====
        boolean mesaLiberada = false;
        String mesaCodigo = null;

        if (orden.getMesa() != null) {
            mesaCodigo = orden.getMesa().getCodigo();
            if (ordenCerrada) {
                liberarMesa(orden.getMesa());
                mesaLiberada = true;
            }
        }

        // ===== 7. GUARDAR CAMBIOS =====
        ordenRepository.save(orden);

        // ===== 8. CONSTRUIR RESPONSE =====
        return responseBuilder
            .ordenId(orden.getId())
            .ordenNumero(orden.getNumero())
            .ordenCerrada(ordenCerrada)
            .ordenEstado(orden.getEstado().name())
            .mesaLiberada(mesaLiberada)
            .mesaCodigo(mesaCodigo)
            .itemsPagados(itemsAPagar.stream().map(OrdenItem::getId).toList())
            .itemsPendientesCount(orden.getItemsPendientes().size())
            .totalPendienteOrden(orden.getTotalPendiente())
            .totalPagadoOrden(orden.getTotalPagado())
            .totalFacturasEmitidas(orden.getCantidadFacturasEmitidas())
            .fechaPago(LocalDateTime.now())
            .mensaje(ordenCerrada
                ? "✅ Orden completamente pagada y cerrada"
                : "✅ Pago parcial procesado. Quedan " + orden.getItemsPendientes().size() + " items pendientes")
            .build();
    }

    // =============================================
    // MÉTODOS DE VALIDACIÓN
    // =============================================

    private Orden validarOrdenParaPago(Long ordenId) {
        Orden orden = ordenRepository.findById(ordenId)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + ordenId));

        if (orden.getEstado().esFinal()) {
            throw new BusinessException(
                "La orden " + orden.getNumero() + " ya está cerrada (estado: " + orden.getEstado() + ")"
            );
        }

        if (!orden.getEstado().puedePagarse()) {
            throw new BusinessException(
                "La orden " + orden.getNumero() + " no puede pagarse en estado: " + orden.getEstado()
            );
        }

        return orden;
    }

    private List<OrdenItem> validarItemsParaPago(Orden orden, List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            throw new BadRequestException("Debe seleccionar al menos un item para pagar");
        }

        Set<Long> itemIdsSet = Set.copyOf(itemIds);

        List<OrdenItem> itemsAPagar = orden.getItems().stream()
            .filter(item -> itemIdsSet.contains(item.getId()))
            .toList();

        if (itemsAPagar.size() != itemIds.size()) {
            Set<Long> encontrados = itemsAPagar.stream()
                .map(OrdenItem::getId)
                .collect(Collectors.toSet());

            List<Long> noEncontrados = itemIds.stream()
                .filter(id -> !encontrados.contains(id))
                .toList();

            throw new ResourceNotFoundException("Items no encontrados en la orden: " + noEncontrados);
        }

        List<OrdenItem> yaPagados = itemsAPagar.stream()
            .filter(OrdenItem::estaPagado)
            .toList();

        if (!yaPagados.isEmpty()) {
            String itemsStr = yaPagados.stream()
                .map(i -> i.getProducto().getNombre())
                .collect(Collectors.joining(", "));

            throw new BusinessException("Los siguientes items ya fueron pagados: " + itemsStr);
        }

        return itemsAPagar;
    }

    private void validarMediosPago(PagoParcialRequest request) {
        if (request.getMediosPago() == null || request.getMediosPago().isEmpty()) {
            throw new BadRequestException("Debe especificar al menos un medio de pago");
        }

        BigDecimal totalMediosPago = request.getMediosPago().stream()
            .map(PagoParcialRequest.MedioPagoItemRequest::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalMediosPago.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("El monto total de los medios de pago debe ser mayor a cero");
        }
    }

    // =============================================
    // CÁLCULOS
    // =============================================

    private BigDecimal calcularTotalItems(List<OrdenItem> items) {
        return items.stream()
            .map(OrdenItem::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // =============================================
    // GENERACIÓN DE FACTURAS
    // =============================================

    private FacturaInterna generarFacturaInterna(
        Orden orden,
        List<OrdenItem> items,
        PagoParcialRequest request,
        Long usuarioId,
        Long sesionCajaId) {

        log.info("📄 Generando factura interna para {} items", items.size());

        // Construir detalles
        List<DetalleFacturaInternaRequest> detalles = items.stream()
            .map(item -> {
                DetalleFacturaInternaRequest detalle = new DetalleFacturaInternaRequest();
                detalle.setProductoId(item.getProducto().getId());
                detalle.setCantidad(item.getCantidad());
                detalle.setPrecioUnitario(item.getPrecioUnitario());
                detalle.setSubtotal(item.getSubtotal());
                detalle.setDescuento(item.getTotalDescuento() != null ? item.getTotalDescuento() : BigDecimal.ZERO);
                detalle.setNotas(item.getNotas());
                return detalle;
            })
            .toList();

        // Construir medios de pago
        List<MedioPagoInternoRequest> mediosPago = request.getMediosPago().stream()
            .map(mp -> {
                MedioPagoInternoRequest medio = new MedioPagoInternoRequest();
                medio.setTipoPago(mp.getTipo());
                medio.setMonto(mp.getMonto());
                medio.setReferencia(mp.getReferencia());
                medio.setBanco(mp.getBanco());
                return medio;
            })
            .toList();

        // Calcular total recibido
        BigDecimal totalRecibido = request.getMontoRecibido() != null
            ? request.getMontoRecibido()
            : mediosPago.stream()
                .map(MedioPagoInternoRequest::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Construir request
        CrearFacturaInternaRequest facturaRequest = new CrearFacturaInternaRequest();
        facturaRequest.setEmpresaId(orden.getSucursal().getEmpresa().getId());
        facturaRequest.setSucursalId(request.getSucursalId());
        facturaRequest.setUsuarioId(usuarioId);
        facturaRequest.setSesionCajaId(sesionCajaId);
        facturaRequest.setOrdenId(orden.getId());
        facturaRequest.setClienteId(request.getClienteId());
        facturaRequest.setNombreCliente(request.getNombreCliente());
        facturaRequest.setDetalles(detalles);
        facturaRequest.setMediosPago(mediosPago);
        facturaRequest.setPagoRecibido(totalRecibido);
        facturaRequest.setNotas(request.getNotas() != null
            ? request.getNotas()
            : "Pago parcial - Orden " + orden.getNumero());
        facturaRequest.setNumeroViper(request.getNumeroViper());

        // Crear factura
        var response = facturaInternaService.crear(facturaRequest);

        log.info("✅ Factura interna generada: {}", response.getNumero());

        // Recuperar entidad completa
        return facturaInternaService.buscarPorNumero(response.getNumero())
            .orElseThrow(() -> new BusinessException(
                "Error recuperando factura interna: " + response.getNumero()
            ));
    }

    // =============================================
    // ACTUALIZACIÓN DE ESTADOS
    // =============================================

    private void marcarItemsPagadosConFacturaInterna(List<OrdenItem> items, FacturaInterna factura) {
        for (OrdenItem item : items) {
            item.marcarPagadoConFacturaInterna(factura);
            ordenItemRepository.save(item);
            log.debug("✅ Item {} marcado como pagado", item.getId());
        }
    }

    private boolean actualizarEstadoOrden(Orden orden) {
        if (orden.todosItemsPagados()) {
            orden.setEstado(EstadoOrden.PAGADA);
            orden.setFechaCierre(LocalDateTime.now());
            log.info("✅ Orden {} cerrada - todos los items pagados", orden.getNumero());
            return true;
        } else {
            if (orden.getEstado() != EstadoOrden.POR_PAGAR) {
                orden.setEstado(EstadoOrden.POR_PAGAR);
                log.info("📝 Orden {} actualizada a POR_PAGAR", orden.getNumero());
            }
            return false;
        }
    }

    private void liberarMesa(Mesa mesa) {
        mesa.actualizarEstadoSegunOrden();
        mesaRepository.save(mesa);
        log.info("🪑 Mesa {} liberada", mesa.getCodigo());
    }

    // =============================================
    // CONSULTAS
    // =============================================

    @Transactional(readOnly = true)
    public OrdenEstadoPagosResponse obtenerEstadoPagos(Long ordenId) {
        log.info("🔍 Consultando estado de pagos para orden: {}", ordenId);

        Orden orden = ordenRepository.findById(ordenId)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + ordenId));

        // Mapear items
        List<OrdenEstadoPagosResponse.ItemEstadoPagoDTO> itemsDTO = orden.getItems().stream()
            .map(this::mapItemEstadoPago)
            .toList();

        // Mapear facturas emitidas
        List<OrdenEstadoPagosResponse.FacturaResumenDTO> facturasDTO = new ArrayList<>();

        for (FacturaInterna fi : orden.getFacturasInternasParciales()) {
            facturasDTO.add(OrdenEstadoPagosResponse.FacturaResumenDTO.builder()
                .id(fi.getId())
                .tipo("INTERNA")
                .tipoDocumento(fi.getNumero().startsWith("TI-") ? "TI" : "FI")
                .numero(fi.getNumero())
                .total(fi.getTotal())
                .fecha(fi.getFecha())
                .cantidadItems(fi.getDetalles().size())
                .build());
        }

        for (Factura f : orden.getFacturasParciales()) {
            facturasDTO.add(OrdenEstadoPagosResponse.FacturaResumenDTO.builder()
                .id(f.getId())
                .tipo("ELECTRONICA")
                .tipoDocumento(f.getTipoDocumento().name())
                .numero(f.getConsecutivo())
                .total(f.getTotalComprobante())
                .fecha(LocalDateTime.parse(f.getFechaEmision().substring(0, 19)))
                .cantidadItems(f.getDetalles().size())
                .build());
        }

        // Calcular totales
        BigDecimal totalPagado = orden.getTotalPagado();
        BigDecimal totalPendiente = orden.getTotalPendiente();
        BigDecimal totalOrden = orden.getTotal();

        BigDecimal porcentajePagado = BigDecimal.ZERO;
        if (totalOrden.compareTo(BigDecimal.ZERO) > 0) {
            porcentajePagado = totalPagado
                .multiply(new BigDecimal("100"))
                .divide(totalOrden, 2, RoundingMode.HALF_UP);
        }

        return OrdenEstadoPagosResponse.builder()
            .ordenId(orden.getId())
            .ordenNumero(orden.getNumero())
            .mesaCodigo(orden.getMesa() != null ? orden.getMesa().getCodigo() : null)
            .estado(orden.getEstado().name())
            .totalOrden(totalOrden)
            .totalPagado(totalPagado)
            .totalPendiente(totalPendiente)
            .porcentajePagado(porcentajePagado)
            .items(itemsDTO)
            .itemsTotales(orden.getItems().size())
            .itemsPagados(orden.getItemsPagados().size())
            .itemsPendientes(orden.getItemsPendientes().size())
            .facturasEmitidas(facturasDTO)
            .totalFacturasEmitidas(facturasDTO.size())
            .build();
    }

    private OrdenEstadoPagosResponse.ItemEstadoPagoDTO mapItemEstadoPago(OrdenItem item) {
        String facturaNumero = null;
        Long facturaId = null;
        String tipoDocPago = null;

        if (item.getFacturaInterna() != null) {
            facturaId = item.getFacturaInterna().getId();
            facturaNumero = item.getFacturaInterna().getNumero();
            tipoDocPago = facturaNumero.startsWith("TI-") ? "TI" : "FI";
        } else if (item.getFactura() != null) {
            facturaId = item.getFactura().getId();
            facturaNumero = item.getFactura().getConsecutivo();
            tipoDocPago = item.getFactura().getTipoDocumento().name();
        }

        return OrdenEstadoPagosResponse.ItemEstadoPagoDTO.builder()
            .itemId(item.getId())
            .productoId(item.getProducto().getId())
            .productoNombre(item.getProducto().getNombre())
            .cantidad(item.getCantidad())
            .precioUnitario(item.getPrecioUnitario())
            .total(item.getTotal())
            .estadoPago(item.getEstadoPago() != null ? item.getEstadoPago().name() : "PENDIENTE")
            .fechaPago(item.getFechaPago())
            .tipoDocumentoPago(tipoDocPago)
            .facturaId(facturaId)
            .facturaNumero(facturaNumero)
            .build();
    }
}