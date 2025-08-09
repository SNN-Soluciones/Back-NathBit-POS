package com.snnsoluciones.backnathbitpos.dto.usuario;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import lombok.Data;

@Data
public class AccesoDTO {
    private EmpresaInfo empresa;
    private SucursalInfo sucursal;
    private RolNombre rol; // Rol global del usuario
    private Boolean accesoTodasSucursales; // Si tiene acceso a todas las sucursales

    @Data
    public static class EmpresaInfo {
        private Long id;
        private String nombre;
        private String codigo;
        private String logoUrl;
    }

    @Data
    public static class SucursalInfo {
        private Long id;
        private String nombre;
        private String codigo;
    }
}