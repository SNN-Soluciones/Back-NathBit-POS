package com.snnsoluciones.backnathbitpos.enums;

/**
 * Rol que juega un producto o categoría dentro de una promoción.
 *
 * TRIGGER  → Su presencia en la orden ACTIVA la promoción.
 *            Ejemplos:
 *              - Las 2 hamburguesas en un NXM
 *              - Los 2 adultos en un GRUPO_CONDICIONAL
 *              - El ítem "Promo Alitas AYCE" en un ALL_YOU_CAN_EAT
 *
 * BENEFICIO → Recibe el descuento o sale gratis.
 *             Ejemplos:
 *               - La soda gratis en un NXM cruzado
 *               - Lo que come el niño en un GRUPO_CONDICIONAL
 *               - Si TRIGGER y BENEFICIO son el mismo alcance
 *                 (2+1 de lo mismo), ambos roles apuntan al mismo grupo
 */
public enum RolPromocionAlcance {
    TRIGGER,
    BENEFICIO
}