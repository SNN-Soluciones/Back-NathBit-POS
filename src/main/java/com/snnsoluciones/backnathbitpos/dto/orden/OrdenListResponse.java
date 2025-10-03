package com.snnsoluciones.backnathbitpos.dto.orden;

import com.snnsoluciones.backnathbitpos.entity.OrdenItem;
import com.snnsoluciones.backnathbitpos.enums.EstadoOrden;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrdenListResponse(
    Long id,
    String numero,
    String mesaCodigo,
    String meseroNombre,
    EstadoOrden estado,
    Integer cantidadItems,
    BigDecimal total,
    LocalDateTime fechaCreacion,
    Integer tiempoTranscurridoMinutos,
    List<OrdenItemResumenResponse> items
) {}
