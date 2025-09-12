package com.snnsoluciones.backnathbitpos.dto.usuarios;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PerfilUpdateRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    @NotBlank(message = "Los apellidos son obligatorios")
    private String apellidos;
    
    private String telefono;
}