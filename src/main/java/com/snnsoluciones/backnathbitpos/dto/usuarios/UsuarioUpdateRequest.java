package com.snnsoluciones.backnathbitpos.dto.usuarios;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UsuarioUpdateRequest {
    
    private String email;
    
    private String password; // Opcional para update
    
    private String nombre;
    
    private String apellidos;
    
    private RolNombre rol;
    
    private Boolean activo;

    private String telefono;

}