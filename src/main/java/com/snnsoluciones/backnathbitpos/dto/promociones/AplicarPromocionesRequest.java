package com.snnsoluciones.backnathbitpos.dto.promociones;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request para aplicar promos seleccionadas a una orden.
 * El mesero elige cuáles de las promociones disponibles quiere aplicar.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AplicarPromocionesRequest {

    @NotEmpty(message = "Debe seleccionar al menos una promoción")
    private List<Long> promocionIds;
}