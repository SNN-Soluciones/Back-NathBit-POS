package com.snnsoluciones.backnathbitpos.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.snnsoluciones.backnathbitpos.dto.empresa.EmpresaResumenDTO;
import com.snnsoluciones.backnathbitpos.dto.empresa.SucursalResumenDTO;
import com.snnsoluciones.backnathbitpos.dto.usuario.UsuarioDTO;
import lombok.*;

import java.util.List;

/**
 * Response del login adaptado para el nuevo modelo de un rol por usuario
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    // Tokens
    private String token;
    private String refreshToken;

    // Información del usuario
    private UsuarioDTO usuario;

    // Contexto para usuarios operativos (acceso directo)
    private ContextoOperativoDTO contexto;

    // Empresas disponibles para usuarios administrativos
    private List<EmpresaAccesoDTO> empresas;

    // Flags de control
    private Boolean requiereSeleccion;
    private String rutaDestino;
    private String tipoAcceso; // SISTEMA, EMPRESARIAL, OPERATIVO

    /**
     * DTO para el contexto de usuarios operativos
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContextoOperativoDTO {
        private EmpresaResumenDTO empresa;
        private SucursalResumenDTO sucursal;
        private PermisosDTO permisos;
    }

    /**
     * DTO para empresas con acceso
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmpresaAccesoDTO {
        private Long id;
        private String codigo;
        private String nombre;
        private String nombreComercial;
        private String logo;
        private Boolean activa;
        private List<SucursalAccesoDTO> sucursales;
        private Integer totalSucursales;
        private Boolean accesoTodasSucursales;
    }

    /**
     * DTO para sucursales con acceso
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SucursalAccesoDTO {
        private Long id;
        private String codigo;
        private String nombre;
        private String direccion;
        private Boolean activa;
        private Boolean esPrincipal;
    }

    /**
     * DTO para permisos
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermisosDTO {
        private Boolean accesoTotal;
        private ModuloPermisosDTO productos;
        private ModuloPermisosDTO ordenes;
        private ModuloPermisosDTO caja;
        private ModuloPermisosDTO reportes;
        private ModuloPermisosDTO clientes;
        private ModuloPermisosDTO inventario;
        private ModuloPermisosDTO usuarios;
        private ModuloPermisosDTO configuracion;
    }

    /**
     * DTO para permisos por módulo
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModuloPermisosDTO {
        private Boolean ver;
        private Boolean crear;
        private Boolean editar;
        private Boolean eliminar;
        private Boolean imprimir;
        private Boolean exportar;
    }
}