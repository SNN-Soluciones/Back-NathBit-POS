package com.snnsoluciones.backnathbitpos.dto.dispositivo;

import lombok.*;

import java.util.List;

/**
 * Response con lista de usuarios disponibles para login en el PDV
 * Endpoint: GET /api/dispositivos/usuarios
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispositivoUsuariosResponse {

    /**
     * Información de la empresa
     */
    private EmpresaInfo empresa;

    /**
     * Información de la sucursal
     */
    private SucursalInfo sucursal;

    /**
     * Lista de usuarios que pueden hacer login en este PDV
     */
    private List<UsuarioInfo> usuarios;

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
        private String rol;
        private Boolean requiereCambioPin;
        private String  fuente;

        /**
         * Indica si el usuario tiene PIN configurado
         */
        private Boolean tienePin;

        /**
         * Longitud del PIN (para mostrar cantidad de dígitos en el teclado)
         */
        private Integer longitudPin;

        /**
         * Indica si el usuario tiene entrada activa hoy (no ha marcado salida)
         */
        private Boolean tieneEntradaActiva;

        /**
         * Color del avatar (generado según ID)
         */
        private String avatarColor;
    }
}