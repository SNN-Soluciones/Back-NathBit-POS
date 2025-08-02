package com.snnsoluciones.backnathbitpos.enums;

/**
 * Enum que define los roles disponibles en el sistema
 */
public enum RolNombre {
    SUPER_ADMIN,    // Puede gestionar múltiples tenants
    ADMIN,          // Admin del restaurante (tenant)
    JEFE_CAJAS,     // Puede abrir/cerrar cajas, eliminar líneas, autorizar descuentos
    CAJERO,         // Puede cobrar y manejar caja
    MESERO,         // Puede tomar órdenes
    COCINA,         // Ve órdenes para preparar
    CLIENTE         // Para futuros pedidos online
}