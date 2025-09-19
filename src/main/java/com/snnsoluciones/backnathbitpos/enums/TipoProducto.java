package com.snnsoluciones.backnathbitpos.enums;

/**
 * Tipos de productos en el sistema
 * Define la naturaleza y comportamiento del producto
 */
public enum TipoProducto {

    /**
     * Productos con inventario simple que pueden venderse directamente
     * O usarse como ingrediente en recetas
     * Ejemplo: Coca Cola (se vende como bebida o se usa en Cuba Libre)
     */
    MIXTO,

    /**
     * Productos que solo sirven como ingredientes, no se venden al público
     * Ejemplo: 200g de arroz, perejil, sal, aceite
     */
    MATERIA_PRIMA,

    /**
     * Productos destinados únicamente a la venta
     * Ejemplo: Casado con pollo, botella de agua
     */
    VENTA,

    /**
     * Conjunto fijo de productos con precio especial
     * Ejemplo: Hamburguesa + Papas + Bebida = Combo #1
     */
    COMBO,

    /**
     * Producto personalizable con opciones variables
     * Ejemplo: Papas con curry (tipo papa + tipo carne + salsas)
     */
    COMPUESTO
}