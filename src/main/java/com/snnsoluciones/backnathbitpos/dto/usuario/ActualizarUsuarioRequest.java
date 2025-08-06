package com.snnsoluciones.backnathbitpos.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActualizarUsuarioRequest {
    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe ser válido")
    private String email;
    
    @NotBlank(message = "El nombre es requerido")
    private String nombre;
    
    @NotBlank(message = "Los apellidos son requeridos")
    private String apellidos;
    
    private String telefono;
    private String identificacion;
}
