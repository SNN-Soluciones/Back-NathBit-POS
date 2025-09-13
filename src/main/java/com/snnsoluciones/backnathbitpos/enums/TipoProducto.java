// Archivo: src/main/java/com/snnsoluciones/backnathbitpos/enums/TipoProducto.java

package com.snnsoluciones.backnathbitpos.enums;

public enum TipoProducto {
    VENTA,          // Producto para venta directa (antes PRODUCTO)
    SERVICIO,       // Servicio (lo mantienes)
    COMBO,          // Combo/paquete (lo mantienes)
    MODIFICADOR,    // Modificador/extra (lo mantienes)
    MATERIA_PRIMA,  // Solo materia prima, no se vende
    AMBOS           // Se puede vender Y usar como materia prima (NUEVO)
}