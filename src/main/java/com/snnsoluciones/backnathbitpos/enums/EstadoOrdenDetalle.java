// Archivo: src/main/java/com/snnsoluciones/backnathbitpos/enums/EstadoOrdenDetalle.java

package com.snnsoluciones.backnathbitpos.enums;

public enum EstadoOrdenDetalle {
    PENDIENTE,          // Pedido tomado, esperando preparación
    EN_PREPARACION,     // Siendo preparado en cocina
    LISTO,              // Listo para servir
    SERVIDO,            // Servido al cliente
    CANCELADO,          // Cancelado
    DEVUELTO           // Devuelto por el cliente
}