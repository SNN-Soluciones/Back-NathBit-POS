package com.snnsoluciones.backnathbitpos.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioCreateRequest {
    @NotBlank
    @Email
    private String email;
    
    @NotBlank
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
             message = "La contraseña debe contener mayúsculas, minúsculas y números")
    private String password;
    
    @NotBlank
    private String nombre;
    
    @NotBlank
    private String apellidos;
    
    private String telefono;
    
    @NotEmpty
    private Set<String> roles;
}