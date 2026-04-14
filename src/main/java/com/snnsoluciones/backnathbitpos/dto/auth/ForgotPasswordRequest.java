package com.snnsoluciones.backnathbitpos.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "El email es requerido")
    private String email;
}