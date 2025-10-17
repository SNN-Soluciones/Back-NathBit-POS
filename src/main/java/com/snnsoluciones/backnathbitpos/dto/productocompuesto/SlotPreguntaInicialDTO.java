package com.snnsoluciones.backnathbitpos.dto.productocompuesto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para el slot de pregunta inicial
 * Ejemplo: "¿Cómo deseas tu Birriamen?"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotPreguntaInicialDTO {

    /**
     * ID del slot
     */
    private Long slotId;

    /**
     * Nombre del slot (generalmente "Presentación" o similar)
     */
    private String nombre;

    /**
     * Pregunta que se muestra al usuario
     * Ejemplo: "¿Cómo deseas tu Birriamen?"
     */
    private String pregunta;

    /**
     * Descripción adicional (opcional)
     */
    private String descripcion;

    /**
     * Opciones disponibles (Combo, Sencillo, etc.)
     */
    private List<OpcionPreguntaInicialDTO> opciones;
}