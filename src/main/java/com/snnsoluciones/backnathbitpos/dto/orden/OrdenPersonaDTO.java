package com.snnsoluciones.backnathbitpos.dto.orden;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para representar una persona en una orden
 */
public record OrdenPersonaDTO(
    Long id,
    String nombre,
    String color,
    Integer ordenVisualizacion,
    Integer cantidadItems,
    BigDecimal total,
    String estadoPago, // "PENDIENTE", "PARCIAL", "PAGADO"
    LocalDateTime createdAt,
    List<Long> itemIds // IDs de los items de esta persona
) {}