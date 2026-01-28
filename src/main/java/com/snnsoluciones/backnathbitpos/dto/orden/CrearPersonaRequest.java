package com.snnsoluciones.backnathbitpos.dto.orden;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request para crear una persona en una orden
 */
public record CrearPersonaRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    String nombre,

    @Size(max = 7, message = "El color debe ser formato hex (#RRGGBB)")
    String color, // Opcional, si no viene se genera automático

    Integer ordenVisualizacion // Opcional, para ordenar
) {}