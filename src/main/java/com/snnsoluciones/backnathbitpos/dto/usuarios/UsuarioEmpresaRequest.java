package com.snnsoluciones.backnathbitpos.dto.usuarios;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UsuarioEmpresaRequest {
    
    @NotNull(message = "El usuario es obligatorio")
    private Long usuarioId;
    
    @NotNull(message = "La empresa es obligatoria")
    private Long empresaId;
    
    private Long sucursalId; // Opcional según rol
}
