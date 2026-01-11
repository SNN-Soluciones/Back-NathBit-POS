package com.snnsoluciones.backnathbitpos.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * Request para login con PIN en PDV
 * Endpoint: POST /api/auth/login-pdv
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginPdvRequest {
    
    /**
     * ID del usuario que hace login
     */
    @NotNull(message = "El ID del usuario es obligatorio")
    private Long usuarioId;
    
    /**
     * PIN de 4-6 dígitos
     */
    @NotBlank(message = "El PIN es obligatorio")
    @Pattern(regexp = "^\\d{4,6}$", message = "El PIN debe tener entre 4 y 6 dígitos")
    private String pin;
}