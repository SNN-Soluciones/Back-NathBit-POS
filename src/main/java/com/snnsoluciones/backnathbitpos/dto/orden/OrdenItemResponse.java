package com.snnsoluciones.backnathbitpos.dto.orden;

import com.snnsoluciones.backnathbitpos.enums.ZonaPreparacion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrdenItemResponse(
    Long id,
    Long productoId,
    String productoNombre,
    String productoCodigo,
    BigDecimal cantidad,
    BigDecimal precioUnitario,
    BigDecimal porcentajeDescuento,
    BigDecimal tarifaImpuesto,
    BigDecimal subtotal,
    BigDecimal totalDescuento,
    BigDecimal totalImpuesto,
    BigDecimal total,
    ZonaPreparacion productoZonaPreparacion,
    String notas,
    Boolean enviadoCocina,
    LocalDateTime fechaEnvioCocina,
    Boolean preparado,
    LocalDateTime fechaPreparado,
    Boolean entregado,
    LocalDateTime fechaEntregado,
    List<OrdenItemOpcionResponse> opciones,
    String estadoPago,
    Long facturaInternaId,
    LocalDateTime fechaPago
) {}
