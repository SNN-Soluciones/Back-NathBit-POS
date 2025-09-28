package com.snnsoluciones.backnathbitpos.dto.orden;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ActualizarItemRequest(
    @NotNull @Min(1) Integer cantidad,
    String notas
) {}
