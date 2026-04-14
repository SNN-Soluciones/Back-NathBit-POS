package com.snnsoluciones.backnathbitpos.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank
    private String email;

    @NotBlank
    private String codigo;

    @NotBlank
    @Size(min = 6)
    private String nuevaPassword;

    @NotBlank
    private String confirmarPassword;
}