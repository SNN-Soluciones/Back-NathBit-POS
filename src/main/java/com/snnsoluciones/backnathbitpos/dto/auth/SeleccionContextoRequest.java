package com.snnsoluciones.backnathbitpos.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeleccionContextoRequest {
    @NotNull(message = "La empresa es requerida")
    private Long empresaId;
    
    private Long sucursalId; // Opcional - null = todas las sucursales
}
