package com.snnsoluciones.backnathbitpos.dto.orden;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ItemCocinaResponse(
    Long id,
    String productoNombre,
    BigDecimal cantidad,
    String notas,
    List<String> opciones,
    LocalDateTime fechaEnvioCocina,
    Integer tiempoEsperaMinutos,
    Boolean urgente
) {}