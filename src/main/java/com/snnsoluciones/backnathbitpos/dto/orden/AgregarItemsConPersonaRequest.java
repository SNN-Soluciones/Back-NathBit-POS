package com.snnsoluciones.backnathbitpos.dto.orden;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request para agregar items con persona asignada
 */
public record AgregarItemsConPersonaRequest(
    @NotBlank(message = "El nombre de la persona es obligatorio")
    String nombrePersona,

    String colorPersona, // Opcional

    @NotEmpty(message = "Debe agregar al menos un item")
    @Valid
    List<ItemPersonaRequest> items
) {
    public record ItemPersonaRequest(
        @NotNull(message = "El ID del producto es obligatorio")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        java.math.BigDecimal cantidad,

        String notas
    ) {}
}