// CrearCategoriaMenuItemRequest.java
package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearCategoriaMenuItemRequest {

    @NotNull(message = "El ID del producto hijo es requerido")
    private Long productoHijoId;

    @NotNull(message = "El orden es requerido")
    @PositiveOrZero(message = "El orden debe ser mayor o igual a 0")
    private Integer orden;

    private Boolean destacado;
}
