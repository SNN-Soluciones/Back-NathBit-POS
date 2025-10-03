package com.snnsoluciones.backnathbitpos.dto.orden;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ActualizarItemRequest(
    @NotNull @Min(1) BigDecimal cantidad,
    String notas
) {}
