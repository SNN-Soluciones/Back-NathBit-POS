package com.snnsoluciones.backnathbitpos.dto.dispositivo;

import lombok.*;

/**
 * Response al registrar exitosamente un dispositivo PDV
 * El PDV debe guardar el deviceToken en storage local
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarDispositivoResponse {
    
    /**
     * Token permanente del dispositivo (guardar en Capacitor Preferences)
     * Formato: "DEV-a1b2c3d4-e5f6-7890"
     */
    private String deviceToken;
    
    /**
     * Información de la empresa
     */
    private EmpresaInfo empresa;
    
    /**
     * Información de la sucursal
     */
    private SucursalInfo sucursal;
    
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
        private String nombre;
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