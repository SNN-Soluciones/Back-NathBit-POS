package com.snnsoluciones.backnathbitpos.dto.usuario;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CambiarPasswordRequest {
    
    @NotBlank(message = "La contraseña actual es requerida")
    private String passwordActual;
    
    @NotBlank(message = "La nueva contraseña es requerida")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String passwordNueva;
    
    @NotBlank(message = "Debe confirmar la nueva contraseña")
    private String passwordConfirmacion;
    
    @AssertTrue(message = "Las contraseñas no coinciden")
    private boolean isPasswordsMatch() {
        return passwordNueva != null && passwordNueva.equals(passwordConfirmacion);
    }
}