package com.snnsoluciones.backnathbitpos.dto.orden;

import java.math.BigDecimal;

public record OrdenItemOpcionResponse(
    Long id,
    Long slotId,
    String slotNombre,
    Long productoOpcionId,
    String productoOpcionNombre,
    BigDecimal cantidad,
    BigDecimal precioAdicional,
    Boolean esGratuita
) {}
