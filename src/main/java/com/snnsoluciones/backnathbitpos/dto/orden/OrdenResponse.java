package com.snnsoluciones.backnathbitpos.dto.orden;
// ========== RESPONSES ==========

import com.snnsoluciones.backnathbitpos.enums.EstadoOrden;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrdenResponse(
    Long id,
    String numero,
    Long mesaId,
    String mesaCodigo,
    String zonaNombre,
    Long meseroId,
    String meseroNombre,
    EstadoOrden estado,
    String estadoDescripcion,
    Long clienteId,
    String clienteNombre,
    Integer numeroPersonas,
    BigDecimal porcentajeServicio,
    String observaciones,
    List<OrdenItemResponse> items,
    BigDecimal subtotal,
    BigDecimal totalDescuento,
    BigDecimal totalImpuesto,
    BigDecimal totalServicio,
    BigDecimal total,
    Boolean esSplit,
    Long ordenPadreId,
    LocalDateTime fechaCreacion,
    LocalDateTime fechaActualizacion,
    LocalDateTime fechaCierre,
    Long facturaId,
    String facturaNumero,
    Boolean tienePersonas,
    Integer cantidadPersonas,
    List<OrdenPersonaDTO> personas,
    Integer itemsCompartidos
) {}
