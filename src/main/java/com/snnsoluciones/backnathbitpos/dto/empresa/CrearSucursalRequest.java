package com.snnsoluciones.backnathbitpos.dto.empresa;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CrearSucursalRequest {
    @NotBlank(message = "El código es requerido")
    @Size(min = 2, max = 10, message = "El código debe tener entre 2 y 10 caracteres")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "El código solo puede contener letras mayúsculas y números")
    private String codigo;
    
    @NotBlank(message = "El nombre es requerido")
    private String nombre;
    
    @NotBlank(message = "La dirección es requerida")
    private String direccion;
    
    @NotBlank(message = "El teléfono es requerido")
    private String telefono;
    
    @Email(message = "El email debe ser válido")
    private String email;
}