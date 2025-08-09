package com.snnsoluciones.backnathbitpos.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para solicitar recuperación de contraseña")
public class RecuperarPasswordRequest {
    
    @NotBlank(message = "El email es requerido")
    @Email(message = "Email inválido")
    @Schema(description = "Email del usuario", example = "usuario@ejemplo.com", required = true)
    private String email;
}