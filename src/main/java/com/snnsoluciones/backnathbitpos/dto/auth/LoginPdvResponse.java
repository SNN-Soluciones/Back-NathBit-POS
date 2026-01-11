package com.snnsoluciones.backnathbitpos.dto.auth;

import lombok.*;

/**
 * Response al hacer login con PIN en PDV
 * Contiene el JWT y datos del usuario/empresa/sucursal
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginPdvResponse {
    
    /**
     * JWT token para autenticación de sesión
     */
    private String token;
    
    /**
     * Información del usuario autenticado
     */
    private UsuarioInfo usuario;
    
    /**
     * Información de la empresa
     */
    private EmpresaInfo empresa;
    
    /**
     * Información de la sucursal
     */
    private SucursalInfo sucursal;
    
    /**
     * Indica si el usuario debe cambiar su PIN
     */
    private Boolean requiereCambioPin;
    
    /**
     * Ruta a la que debe redirigir el frontend
     * Ejemplos: "/pos", "/dashboard-admin-empresa/2"
     */
    private String rutaDestino;
    
    /**
     * DTO anidado con info de usuario
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsuarioInfo {
        private Long id;
        private String nombre;
        private String apellidos;
        private String nombreCompleto;
        private String email;
        private String rol;
    }
    
    /**
     * DTO anidado con info de empresa
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmpresaInfo {
        private Long id;
        private String nombreComercial;
    }
    
    /**
     * DTO anidado con info de sucursal
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SucursalInfo {
        private Long id;
        private String nombre;
    }
}