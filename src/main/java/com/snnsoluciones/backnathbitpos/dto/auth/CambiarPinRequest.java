package com.snnsoluciones.backnathbitpos.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * Request para cambiar el PIN del usuario
 * Endpoint: POST /api/auth/cambiar-pin
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CambiarPinRequest {
    
    /**
     * PIN actual (solo si ya tiene PIN configurado)
     */
    private String pinActual;
    
    /**
     * Nuevo PIN de 4-6 dígitos
     */
    @NotBlank(message = "El nuevo PIN es obligatorio")
    @Pattern(regexp = "^\\d{4,6}$", message = "El nuevo PIN debe tener entre 4 y 6 dígitos")
    private String nuevoPin;
    
    /**
     * Confirmación del nuevo PIN
     */
    @NotBlank(message = "La confirmación del PIN es obligatoria")
    @Pattern(regexp = "^\\d{4,6}$", message = "La confirmación debe tener entre 4 y 6 dígitos")
    private String confirmarPin;

    private Integer longitud;
    private String fuente; // "GLOBAL" | "SCHEMA"  ← agregar esto
}