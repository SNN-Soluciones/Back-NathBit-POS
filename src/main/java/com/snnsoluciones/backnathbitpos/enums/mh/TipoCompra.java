package com.snnsoluciones.backnathbitpos.enums.mh;

public enum TipoCompra {
    FACTURA_ELECTRONICA_COMPRA,    // FEC - Tipo 08 generada por nosotros
    FACTURA_PROVEEDOR_INSCRITO,    // Factura recibida de proveedor inscrito
    FACTURA_PROVEEDOR_NO_INSCRITO, // Factura física de proveedor no inscrito
    TIQUETE_COMPRA,                // Tiquete de compra
    IMPORTACION,                   // Documento de importación
    OTRO                           // Otros documentos
}