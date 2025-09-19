package com.snnsoluciones.backnathbitpos.enums;

/**
 * Estados del inventario para control avanzado
 * Permite manejar reservas y bloqueos temporales
 */
public enum EstadoInventario {
    
    /**
     * Inventario disponible para venta
     * Estado normal del producto
     */
    DISPONIBLE,
    
    /**
     * Inventario reservado temporalmente
     * Ej: Producto en mesa pendiente de pago, combo armándose
     */
    BLOQUEADO,
    
    /**
     * Inventario ya vendido/usado
     * Estado final tras confirmar venta
     */
    CONSUMIDO,
    
    /**
     * Inventario que fue bloqueado y se liberó
     * Ej: Cliente canceló orden, mesa liberada
     */
    LIBERADO
}