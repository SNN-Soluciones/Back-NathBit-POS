package com.snnsoluciones.backnathbitpos.dto.usuario;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import lombok.Data;

import java.util.Map;

@Data
public class AccesoDTO {
    private EmpresaInfo empresa;
    private SucursalInfo sucursal;
    private RolNombre rol;
    private Map<String, Map<String, Boolean>> permisos;
    private Boolean esPrincipal;
    
    @Data
    public static class EmpresaInfo {
        private Long id;
        private String nombre;
        private String codigo;
    }
    
    @Data
    public static class SucursalInfo {
        private Long id;
        private String nombre;
        private String codigo;
    }
}