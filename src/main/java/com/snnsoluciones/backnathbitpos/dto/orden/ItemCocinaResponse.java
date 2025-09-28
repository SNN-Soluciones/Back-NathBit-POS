package com.snnsoluciones.backnathbitpos.dto.orden;

import java.time.LocalDateTime;
import java.util.List;

public record ItemCocinaResponse(
    Long id,
    String productoNombre,
    Integer cantidad,
    String notas,
    List<String> opciones,
    LocalDateTime fechaEnvioCocina,
    Integer tiempoEsperaMinutos,
    Boolean urgente
) {}