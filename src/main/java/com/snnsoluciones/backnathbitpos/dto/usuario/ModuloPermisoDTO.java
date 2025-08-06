package com.snnsoluciones.backnathbitpos.dto.usuario;

import lombok.Data;

import java.util.Map;

@Data
public class ModuloPermisoDTO {
    private String modulo;
    private String descripcion;
    private Map<String, Boolean> acciones;
}