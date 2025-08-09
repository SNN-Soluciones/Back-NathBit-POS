package com.snnsoluciones.backnathbitpos.dto.empresa;

import com.snnsoluciones.backnathbitpos.enums.TipoIdentificacion;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmpresaRequest {
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    @NotBlank(message = "El código es obligatorio")
    private String codigo;
    
    private TipoIdentificacion tipoIdentificacion;
    
    private String identificacion;
    
    private String direccion;
    
    private String telefono;
    
    @Email(message = "Email inválido")
    private String email;
    
    private Boolean activa = true;
}
