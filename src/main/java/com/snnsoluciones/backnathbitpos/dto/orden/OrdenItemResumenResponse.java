package com.snnsoluciones.backnathbitpos.dto.orden;

import java.math.BigDecimal;

public record OrdenItemResumenResponse(
    Long productoId,
    String productoNombre,
    BigDecimal cantidad,
    String notas
) {}