package com.snnsoluciones.backnathbitpos.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ValidarPermisoRequest {
    @NotNull(message = "El usuario es requerido")
    private Long usuarioId;
    
    @NotNull(message = "La empresa es requerida")
    private Long empresaId;
    
    private Long sucursalId; // Opcional
    
    @NotBlank(message = "El módulo es requerido")
    private String modulo;
    
    @NotBlank(message = "La acción es requerida")
    private String accion;
}