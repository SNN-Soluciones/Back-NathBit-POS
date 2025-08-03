package com.snnsoluciones.backnathbitpos.dto.auth;

import lombok.*;

/**
 * DTO para la respuesta de selección de contexto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeleccionContextoResponse {
    
    private String accessToken;  // Nuevo token con contexto completo
    private String refreshToken; // Actualizado si es necesario
    private Long expiresIn;
    
    // Información del contexto seleccionado
    private ContextoSeleccionado contexto;
    
    // URL para redirección según el rol
    private String urlRedirect;
}