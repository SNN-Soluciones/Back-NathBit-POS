package com.snnsoluciones.backnathbitpos.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ContextoRequest {
    
    @NotNull(message = "La empresa es obligatoria")
    private Long empresaId;
    
    private Long sucursalId; // Opcional según el rol
}