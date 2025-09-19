package com.snnsoluciones.backnathbitpos.enums;

/**
 * Tipos de control de inventario
 * Define cómo se gestiona el stock del producto
 */
public enum TipoInventario {
    
    /**
     * Control unitario tradicional (1+1-1=1)
     * Para productos con stock físico
     */
    SIMPLE,
    
    /**
     * Se produce con ingredientes según receta
     * Valida disponibilidad de ingredientes
     */
    RECETA,
    
    /**
     * Combo con stock propio pre-armado
     * Ejemplo: 100 cajas navideñas listas
     */
    PROPIO,
    
    /**
     * Combo sin stock, usa componentes
     * Valida stock de cada componente individual
     */
    REFERENCIA,
    
    /**
     * Sin inventario (servicios, compuestos)
     * No requiere control de stock
     */
    NINGUNO
}