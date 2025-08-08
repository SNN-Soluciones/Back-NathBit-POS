package com.snnsoluciones.backnathbitpos.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request para login de usuario")
public class LoginRequest {

    @NotBlank(message = "Email o username es requerido")
    @Schema(description = "Email o username del usuario",
        example = "admin@restaurante.com o admin_rest")
    private String email; // Mantener el nombre por compatibilidad, pero acepta email o username

    @NotBlank(message = "Password es requerido")
    @Schema(description = "Contraseña del usuario", example = "Password123!")
    private String password;
}