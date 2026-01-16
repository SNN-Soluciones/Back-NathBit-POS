// ActualizarCategoriaMenuItemRequest.java
package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.PositiveOrZero;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActualizarCategoriaMenuItemRequest {

    @PositiveOrZero(message = "El orden debe ser mayor o igual a 0")
    private Integer orden;

    private Boolean destacado;
}
