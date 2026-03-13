package com.snnsoluciones.backnathbitpos.enums;

/**
 * Define cómo se selecciona el ítem que sale gratis en una promo NXM.
 *
 * MAS_BARATO          → De todos los ítems que califican como BENEFICIO
 *                       en la orden, el sistema elige automáticamente
 *                       el de menor precio unitario.
 *                       Ejemplo: "Lleva 3 pizzas, la más barata es gratis"
 *
 * PRODUCTO_ESPECIFICO → El ítem gratis está definido explícitamente
 *                       en PromocionProducto con rol BENEFICIO.
 *                       Puede ser un producto distinto al del trigger.
 *                       Ejemplo: "Compra 2 hamburguesas → 1 soda gratis"
 *
 * Nota: este campo solo es relevante para TipoPromocion.NXM.
 * Para GRUPO_CONDICIONAL el criterio del beneficio lo maneja
 * CriterioDescuento.
 */
public enum CriterioItemGratis {
    MAS_BARATO,
    PRODUCTO_ESPECIFICO
}