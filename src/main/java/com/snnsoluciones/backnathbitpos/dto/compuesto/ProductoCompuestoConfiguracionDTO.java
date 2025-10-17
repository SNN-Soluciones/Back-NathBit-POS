package com.snnsoluciones.backnathbitpos.dto.compuesto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO para visualizar una configuración condicional completa
 * Incluye los slots asociados con sus reglas (overrides)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoCompuestoConfiguracionDTO {

    private Long id;

    // Relación con producto compuesto
    private Long compuestoId;

    // Datos básicos
    private String nombre;
    private String descripcion;
    private Integer orden;
    private Boolean activa;

    // Opción que activa esta configuración
    private Long opcionTriggerId;
    private Long opcionTriggerProductoId;
    private String opcionTriggerProductoNombre;

    // Slots asociados con sus overrides
    @Builder.Default
    private List<SlotConfiguracionDTO> slots = new ArrayList<>();

    // Auditoría
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Metadata útil para el frontend
    public Integer getCantidadSlots() {
        return slots != null ? slots.size() : 0;
    }

    public Boolean getTieneOverrides() {
        if (slots == null) return false;
        return slots.stream().anyMatch(SlotConfiguracionDTO::getTieneOverrides);
    }
}