package com.snnsoluciones.backnathbitpos.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CambiarRolRequest {
    
    @NotBlank(message = "El nuevo rol es requerido")
    private String nuevoRol;
    
    private String rolAnterior; // Para auditoría
    
    private String motivo; // Razón del cambio
}
