package com.snnsoluciones.backnathbitpos.dto.promociones;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para solicitar una nueva ronda en AYCE o BARRA_LIBRE.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NuevaRondaRequest {

    @NotNull(message = "El ID de la promoción es obligatorio")
    private Long promocionId;

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId;
}