package com.snnsoluciones.backnathbitpos.dto.orden;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record OpcionCompuestaRequest(
    @NotNull Long slotId,
    @NotNull Long productoOpcionId,
    BigDecimal cantidad
) {}
