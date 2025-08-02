package com.snnsoluciones.backnathbitpos.enums;

/**
 * Enum que define los permisos específicos del sistema
 */
public enum PermisoNombre {
    // Permisos de Caja
    ABRIR_CAJA,
    CERRAR_CAJA,
    AUTORIZAR_DESCUENTO,
    ELIMINAR_LINEA_ORDEN,
    ANULAR_FACTURA,
    REIMPRIMIR_FACTURA,
    VER_REPORTES_CAJA,
    
    // Permisos de Gestión
    GESTIONAR_USUARIOS,
    GESTIONAR_PRODUCTOS,
    GESTIONAR_CATEGORIAS,
    GESTIONAR_MESAS,
    GESTIONAR_CLIENTES,
    
    // Permisos de Órdenes
    CREAR_ORDEN,
    MODIFICAR_ORDEN,
    CANCELAR_ORDEN,
    CAMBIAR_MESA,
    
    // Permisos de Reportes
    VER_REPORTE_VENTAS,
    VER_REPORTE_INVENTARIO,
    VER_REPORTE_EMPLEADOS,
    EXPORTAR_REPORTES,
    
    // Permisos de Sistema
    GESTIONAR_TENANT,
    CONFIGURAR_SISTEMA,
    VER_LOGS_AUDITORIA,
    BACKUP_SISTEMA
}