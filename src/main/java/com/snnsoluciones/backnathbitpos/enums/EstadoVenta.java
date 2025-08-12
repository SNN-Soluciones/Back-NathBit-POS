package com.snnsoluciones.backnathbitpos.enums;

public enum EstadoVenta {
    PENDIENTE,      // En proceso de agregar items
    PAGADA,         // Pagada pero no facturada
    FACTURADA,      // Factura emitida
    ANULADA,        // Venta anulada
    DEVUELTA        // Con devolución total
}