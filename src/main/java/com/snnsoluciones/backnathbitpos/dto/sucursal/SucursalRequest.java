package com.snnsoluciones.backnathbitpos.dto.sucursal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SucursalRequest {
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    @NotBlank(message = "El código es obligatorio")
    private String codigo;
    
    private String direccion;
    
    private String telefono;
    
    @Email(message = "Email inválido")
    private String email;
    
    @NotNull(message = "La empresa es obligatoria")
    private Long empresaId;
    
    private Boolean activa = true;
}