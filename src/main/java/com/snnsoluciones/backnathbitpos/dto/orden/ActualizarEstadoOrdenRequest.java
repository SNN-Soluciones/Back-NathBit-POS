package com.snnsoluciones.backnathbitpos.dto.orden;

import com.snnsoluciones.backnathbitpos.enums.EstadoOrden;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoOrdenRequest(
    @NotNull EstadoOrden nuevoEstado,
    String motivo
) {}
