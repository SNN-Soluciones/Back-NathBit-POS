package com.snnsoluciones.backnathbitpos.enums;

/**
 * Define qué tipo de beneficio recibe el ítem en el grupo BENEFICIO.
 *
 * Aplica para:
 *   - TipoPromocion.GRUPO_CONDICIONAL → qué recibe el "niño"
 *   - TipoPromocion.NXM               → cuando el beneficio no es
 *                                        simplemente gratis sino con %
 *
 * GRATIS      → precioUnitario del ítem beneficiado = 0.
 *               El descuento es del 100%.
 *
 * PORCENTAJE  → Se aplica porcentaje sobre el precioUnitario del ítem
 *               beneficiado. El valor viene en Promocion.valorBeneficio.
 *               Ejemplo: 50% de descuento en el plato del niño.
 *
 * MONTO_FIJO  → Se resta un monto fijo al precioUnitario del ítem
 *               beneficiado. El valor viene en Promocion.valorBeneficio.
 *               Ejemplo: $500 de descuento en el plato del niño.
 */
public enum CriterioDescuento {
    GRATIS,
    PORCENTAJE,
    MONTO_FIJO
}