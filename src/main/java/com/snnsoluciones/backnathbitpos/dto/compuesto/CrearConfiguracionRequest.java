package com.snnsoluciones.backnathbitpos.dto.compuesto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Request para crear una nueva configuración condicional
 * Usa slots existentes del ProductoCompuesto y permite sobrescribir sus reglas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearConfiguracionRequest {

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;
    
    @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
    private String descripcion;
    
    @NotNull(message = "La opción trigger es requerida")
    private Long opcionTriggerId;
    
    @Builder.Default
    private Integer orden = 0;
    
    @Builder.Default
    private Boolean activa = true;
    
    @NotEmpty(message = "Debe incluir al menos un slot")
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
        
        @Builder.Default
        private Integer orden = 0;
        
        // Overrides opcionales (null = usar valor del slot base)
        private Integer cantidadMinimaOverride;
        private Integer cantidadMaximaOverride;
        private Boolean esRequeridoOverride;
        private BigDecimal precioAdicionalOverride;
    }
}