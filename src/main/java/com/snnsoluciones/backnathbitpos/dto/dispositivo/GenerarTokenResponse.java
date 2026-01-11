package com.snnsoluciones.backnathbitpos.dto.dispositivo;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Response al generar un token de registro de PDV
 * Contiene el token, QR code URL y link de registro
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerarTokenResponse {
    
    /**
     * Token único para el registro (formato: REG-xxxxx)
     */
    private String token;
    
    /**
     * URL para obtener el QR code
     * Ejemplo: "https://api.nathbit.com/qr/REG-f47ac10b"
     */
    private String qrCodeUrl;
    
    /**
     * Link completo de registro para el PDV
     * Ejemplo: "https://pos.nathbit.com/register?token=REG-f47ac10b"
     */
    private String registrationUrl;
    
    /**
     * Fecha y hora de expiración del token (24 horas)
     */
    private LocalDateTime expiraEn;
    
    /**
     * Nombre del dispositivo que se va a registrar
     */
    private String nombreDispositivo;
    
    /**
     * Nombre de la sucursal
     */
    private String sucursalNombre;
}