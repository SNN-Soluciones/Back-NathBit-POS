package com.snnsoluciones.backnathbitpos.enums;

/**
 * Zona donde se prepara o despacha el producto
 * Determina dónde se imprime o muestra la comanda
 */
public enum ZonaPreparacion {
    
    /**
     * Productos que se preparan en la barra
     * Ejemplo: Bebidas, cafés, cócteles
     */
    BARRA,
    
    /**
     * Productos que se preparan en la cocina
     * Ejemplo: Platos principales, entradas
     */
    COCINA,
    
    /**
     * Productos que no requieren preparación
     * Ejemplo: Productos envasados, servicios
     */
    NINGUNA,
    
    /**
     * Productos de panadería/pastelería
     * Ejemplo: Postres, panes
     */
    PASTELERIA,
    
    /**
     * Área de parrilla
     * Ejemplo: Carnes a la parrilla
     */
    PARRILLA,
    
    /**
     * Otras zonas personalizadas
     */
    OTRA
}