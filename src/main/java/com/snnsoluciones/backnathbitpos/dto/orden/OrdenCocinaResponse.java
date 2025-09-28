package com.snnsoluciones.backnathbitpos.dto.orden;

import java.util.List;

public record OrdenCocinaResponse(
    Long ordenId,
    String ordenNumero,
    String mesaCodigo,
    List<ItemCocinaResponse> items
) {}
