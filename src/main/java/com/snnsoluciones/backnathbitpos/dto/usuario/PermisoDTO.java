package com.snnsoluciones.backnathbitpos.dto.usuario;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import lombok.Data;

import java.util.Map;

@Data
public class PermisoDTO {
    private RolNombre rol;
    private Long empresaId;
    private Long sucursalId;
    private Map<String, Map<String, Boolean>> permisos;
    private boolean personalizados;
}