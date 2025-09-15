package com.snnsoluciones.backnathbitpos.dto.mesas;

import com.snnsoluciones.backnathbitpos.enums.EstadoMesa;

public record MesaResponse(
    Long id, String codigo, String nombre, Integer capacidad, Integer orden,
    EstadoMesa estado, Boolean activa, Long zonaId, Long unionGroupId
) {}