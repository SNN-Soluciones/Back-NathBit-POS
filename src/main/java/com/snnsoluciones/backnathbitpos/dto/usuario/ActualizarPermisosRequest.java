package com.snnsoluciones.backnathbitpos.dto.usuario;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class ActualizarPermisosRequest {
    @NotNull(message = "Los permisos son requeridos")
    private Map<String, Map<String, Boolean>> permisos;
}