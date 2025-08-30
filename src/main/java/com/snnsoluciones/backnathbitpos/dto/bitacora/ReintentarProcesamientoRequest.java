package com.snnsoluciones.backnathbitpos.dto.bitacora;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para forzar reintento manual
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReintentarProcesamientoRequest {
    
    @NotNull(message = "El ID de la bitácora es requerido")
    private Long bitacoraId;
    
    // Opcional: motivo del reintento manual
    private String motivo;
    
    // Opcional: reiniciar contador de intentos
    private Boolean reiniciarContador = false;
}