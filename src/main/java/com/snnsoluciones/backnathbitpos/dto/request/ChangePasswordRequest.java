package com.snnsoluciones.backnathbitpos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ChangePasswordRequest {
    @NotBlank
    private String currentPassword;
    
    @NotBlank
    @Size(min = 8)
    private String newPassword;
    
    @NotBlank
    private String confirmPassword;
}