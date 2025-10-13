package com.snnsoluciones.backnathbitpos.dto.compuesto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Request para actualizar una configuración existente
 * Similar a CrearConfiguracionRequest pero permite actualizaciones parciales
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarConfiguracionRequest {

    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;
    
    @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
    private String descripcion;
    
    private Long opcionTriggerId;
    
    private Integer orden;
    
    private Boolean activa;
    
    @Valid
    private List<SlotConfigRequest> slots;
    
    /**
     * Representa un slot dentro de la configuración con sus overrides
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotConfigRequest {
        
        @NotNull(message = "El ID del slot es requerido")
        private Long slotId;
        
        private Integer orden;
        
        // Overrides opcionales (null = usar valor del slot base)
        private Integer cantidadMinimaOverride;
        private Integer cantidadMaximaOverride;
        private Boolean esRequeridoOverride;
        private BigDecimal precioAdicionalOverride;
    }
}