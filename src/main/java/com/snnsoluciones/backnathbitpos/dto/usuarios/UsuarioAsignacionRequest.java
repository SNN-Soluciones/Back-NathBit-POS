package com.snnsoluciones.backnathbitpos.dto.usuarios;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class UsuarioAsignacionRequest {
    
    @NotNull(message = "Debe especificar las empresas")
    private List<Long> empresasIds;
    
    // Para futuro: también sucursales
    private List<Long> sucursalesIds;
    
    // Modo de operación
    private ModoAsignacion modo = ModoAsignacion.AGREGAR;
    
    public enum ModoAsignacion {
        AGREGAR,      // Agrega a las existentes
        REEMPLAZAR,   // Reemplaza todas
        QUITAR        // Quita las especificadas
    }
}