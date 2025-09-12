package com.snnsoluciones.backnathbitpos.dto.usuarios;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CambiarMiPasswordRequest {
    @NotBlank(message = "La contraseña actual es obligatoria")
    private String passwordActual;
    
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 6, message = "La nueva contraseña debe tener al menos 6 caracteres")
    private String passwordNueva;
    
    @NotBlank(message = "La confirmación de contraseña es obligatoria")
    private String passwordConfirmacion;
    
    // Validación personalizada
    public boolean passwordsCoinciden() {
        return passwordNueva != null && passwordNueva.equals(passwordConfirmacion);
    }
}