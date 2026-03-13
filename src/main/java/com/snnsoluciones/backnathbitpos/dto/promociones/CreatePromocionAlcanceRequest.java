package com.snnsoluciones.backnathbitpos.dto.promociones;

import com.snnsoluciones.backnathbitpos.enums.RolPromocionAlcance;
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
 * El campo rol define si este elemento ACTIVA la promo (TRIGGER)
 * o RECIBE el beneficio (BENEFICIO).
 * Si no se envía, el default es TRIGGER.
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

    /**
     * Rol de este producto/categoría/familia dentro de la promo.
     * TRIGGER  → su presencia activa la promo.
     * BENEFICIO → recibe el descuento.
     * Default: TRIGGER (compatibilidad con promos simples existentes).
     */
    @Builder.Default
    private RolPromocionAlcance rol = RolPromocionAlcance.TRIGGER;
}