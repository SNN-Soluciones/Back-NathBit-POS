package com.snnsoluciones.backnathbitpos.dto.dispositivo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request para registrar un dispositivo PDV usando un token
 * Endpoint: POST /api/dispositivos/registrar
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarDispositivoRequest {
    
    /**
     * Token de registro generado por el admin
     * Ejemplo: "REG-f47ac10b-58cc-4372"
     */
    @NotBlank(message = "El token es obligatorio")
    private String token;
    
    /**
     * Información técnica del dispositivo
     */
    @Valid
    private DeviceInfo deviceInfo;
    
    /**
     * DTO anidado con información del dispositivo
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceInfo {
        
        /**
         * UUID único del hardware del dispositivo
         * Se obtiene del plugin de Capacitor
         */
        private String uuid;
        
        /**
         * Modelo del dispositivo
         * Ejemplo: "Samsung Galaxy Tab A8", "iPad Air 2022"
         */
        private String modelo;
        
        /**
         * Plataforma del dispositivo
         * Valores: "ANDROID", "IOS", "WEB"
         */
        private String plataforma;
        
        /**
         * User agent del navegador/app
         */
        private String userAgent;
    }
}