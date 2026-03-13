package com.snnsoluciones.backnathbitpos.enums;

/**
 * Tipos de promoción disponibles en el sistema.
 *
 * NXM              → 2x1, 3x2, etc. El cliente lleva N pero paga M.
 *                    Usa los campos lleva_n / paga_m en Promocion.
 *
 * BARRA_LIBRE      → Precio fijo, consumo libre de bebidas específicas.
 *                    Los productos permitidos se definen en PromocionItem.
 *                    max_rondas = NULL significa sin límite.
 *
 * ALL_YOU_CAN_EAT  → Precio fijo, ítems con reglas por ronda.
 *                    Ejemplo: 4 alas ilimitadas + 2 birras solo una ronda.
 *                    Las reglas se definen en PromocionItem.
 *                    El ítem que activa la promo tiene rol TRIGGER en
 *                    PromocionProducto. Las rondas se rastrean en
 *                    OrdenPromocionEstado.
 *
 * GRUPO_CONDICIONAL → Un grupo de ítems (TRIGGER) habilita un beneficio
 *                     sobre otro grupo (BENEFICIO).
 *                     Ejemplo: 2 adultos comen → 1 niño come gratis.
 *                     Usa: cantidadTrigger, cantidadBeneficio,
 *                          criterioBeneficio, valorBeneficio.
 *
 * PORCENTAJE       → Descuento porcentual sobre productos/categorías
 *                    en alcance. Usa porcentaje_descuento.
 *
 * MONTO_FIJO       → Descuento de monto exacto. Usa monto_descuento.
 *
 * HAPPY_HOUR       → Descuento o precio especial en rango horario.
 *                    Combina con hora_inicio / hora_fin y días activos.
 *
 * ESPECIAL         → Promo personalizada, descripción libre.
 */
public enum TipoPromocion {
    NXM,
    BARRA_LIBRE,
    ALL_YOU_CAN_EAT,
    GRUPO_CONDICIONAL,
    PORCENTAJE,
    MONTO_FIJO,
    HAPPY_HOUR,
    ESPECIAL
}