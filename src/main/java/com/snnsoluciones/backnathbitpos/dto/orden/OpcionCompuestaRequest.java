package com.snnsoluciones.backnathbitpos.dto.orden;

import jakarta.validation.constraints.NotNull;

public record OpcionCompuestaRequest(
    @NotNull Long slotId,
    @NotNull Long productoOpcionId,
    Integer cantidad
) {}
