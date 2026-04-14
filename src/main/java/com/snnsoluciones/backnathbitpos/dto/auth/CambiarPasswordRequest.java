// Crear nuevo DTO para el cambio de contraseña
package com.snnsoluciones.backnathbitpos.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CambiarPasswordRequest {

    @NotBlank(message = "La contraseña actual es obligatoria")
    private String passwordActual;
    
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String nuevaPassword;
    
    @NotBlank(message = "Debe confirmar la contraseña")
    private String confirmarPassword;
}