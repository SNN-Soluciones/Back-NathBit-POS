package com.snnsoluciones.backnathbitpos.dto.orden;

import jakarta.validation.constraints.NotNull;

/**
 * Request para asignar un item existente a una persona
 */
public record AsignarItemAPersonaRequest(
    @NotNull(message = "El ID de la persona es obligatorio")
    Long personaId
) {}