package com.snnsoluciones.backnathbitpos.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ForgotPasswordRequest {

    @Email
    @NotBlank
    private String email;
}