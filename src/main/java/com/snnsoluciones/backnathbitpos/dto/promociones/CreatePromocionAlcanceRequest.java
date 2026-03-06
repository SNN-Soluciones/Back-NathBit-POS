package com.snnsoluciones.backnathbitpos.dto.promociones;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO genérico para definir el alcance de una promoción.
 * Se reutiliza para familias, categorías y productos específicos.
 *
 * Ejemplo de uso en CreatePromocionRequest:
 *   "familias":    [ { "id": 3, "nombre": "Licores" } ]
 *   "categorias":  [ { "id": 7, "nombre": "Cervezas" } ]
 *   "productos":   [ { "id": 42, "nombre": "Ron Centenario 750ml" } ]
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePromocionAlcanceRequest {

    @NotNull(message = "El ID es obligatorio")
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
}