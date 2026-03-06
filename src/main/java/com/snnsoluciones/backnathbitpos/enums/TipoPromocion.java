package com.snnsoluciones.nathbitbusinesscore.model.enums;

/**
 * Tipos de promoción disponibles en el sistema.
 *
 * NXM          → 2x1, 3x2, etc. El cliente lleva N pero paga M.
 *                Usa los campos lleva_n / paga_m en Promocion.
 *
 * BARRA_LIBRE  → Precio fijo, consumo libre de bebidas específicas.
 *                Los productos permitidos se definen en PromocionItem.
 *                max_rondas = NULL significa sin límite.
 *
 * ALL_YOU_CAN_EAT → Precio fijo, ítems con reglas por ronda.
 *                   Ejemplo: 4 alas ilimitadas + 2 birras solo una ronda.
 *                   Las reglas se definen en PromocionItem.
 *
 * PORCENTAJE   → Descuento porcentual sobre el total o producto.
 *                Usa porcentaje_descuento. Validación en frontend.
 *
 * MONTO_FIJO   → Descuento de monto exacto.
 *                Usa monto_descuento. Validación en frontend.
 *
 * ESPECIAL     → Promo personalizada, descripción libre.
 *                Toda la lógica es responsabilidad del frontend.
 */
public enum TipoPromocion {

    NXM,
    BARRA_LIBRE,
    ALL_YOU_CAN_EAT,
    PORCENTAJE,
    MONTO_FIJO,
    ESPECIAL,

    /**
     * HAPPY_HOUR → Descuento o precio especial en rango horario definido.
     *              Combina con hora_inicio / hora_fin y los días activos.
     *              Ejemplo: 2x1 en cervezas de 5pm a 7pm viernes y sábado.
     */
    HAPPY_HOUR
}