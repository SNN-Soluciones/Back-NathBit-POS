package com.snnsoluciones.backnathbitpos.dto.dispositivo;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO para mostrar información de dispositivos en el admin
 * Usado en listados y detalles de dispositivos
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispositivoDTO {
    
    /**
     * ID del dispositivo
     */
    private Long id;
    
    /**
     * Token del dispositivo (no mostrar completo por seguridad)
     */
    private String deviceToken;
    
    /**
     * Nombre del dispositivo
     */
    private String nombre;
    
    /**
     * ID de la empresa
     */
    private Long empresaId;
    
    /**
     * Nombre comercial de la empresa
     */
    private String empresaNombre;
    
    /**
     * ID de la sucursal
     */
    private Long sucursalId;
    
    /**
     * Nombre de la sucursal
     */
    private String sucursalNombre;
    
    /**
     * Modelo del dispositivo
     */
    private String modelo;
    
    /**
     * Plataforma (ANDROID, IOS, WEB)
     */
    private String plataforma;
    
    /**
     * Indica si está activo
     */
    private Boolean activo;
    
    /**
     * Último uso del dispositivo
     */
    private LocalDateTime ultimoUso;
    
    /**
     * Fecha de registro
     */
    private LocalDateTime createdAt;
    
    /**
     * Token parcial para mostrar (ej: "DEV-a1b2...7890")
     */
    public String getTokenParcial() {
        if (deviceToken == null || deviceToken.length() < 15) {
            return deviceToken;
        }
        String prefix = deviceToken.substring(0, 12);
        String suffix = deviceToken.substring(deviceToken.length() - 4);
        return prefix + "..." + suffix;
    }
}