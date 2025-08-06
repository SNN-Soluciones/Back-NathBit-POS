package com.snnsoluciones.backnathbitpos.dto.usuario;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CrearUsuarioRequest {
    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe ser válido")
    private String email;
    
    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;
    
    @NotBlank(message = "El nombre es requerido")
    private String nombre;
    
    @NotBlank(message = "Los apellidos son requeridos")
    private String apellidos;
    
    private String telefono;
    private String identificacion;
    
    @NotNull(message = "La empresa es requerida")
    private Long empresaId;
    
    private Long sucursalId; // Opcional
    
    @NotNull(message = "El rol es requerido")
    private RolNombre rol;
}