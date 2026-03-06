package com.snnsoluciones.backnathbitpos.dto.promociones;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para crear o actualizar un ítem dentro de una promo.
 * Se usa como lista anidada dentro de CreatePromocionRequest.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePromocionItemRequest {

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId;

    @NotBlank(message = "El nombre del producto es obligatorio")
    private String nombreProducto;

    @NotNull(message = "La cantidad por ronda es obligatoria")
    @Min(value = 1, message = "La cantidad por ronda debe ser al menos 1")
    private Integer cantidadPorRonda;

    /**
     * NULL = ilimitado.
     * N    = máximo N rondas permitidas.
     */
    @Min(value = 1, message = "max_rondas debe ser al menos 1 si se especifica")
    private Integer maxRondas;
}