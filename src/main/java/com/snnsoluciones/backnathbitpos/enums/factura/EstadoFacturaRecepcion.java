package com.snnsoluciones.backnathbitpos.enums.factura;

public enum EstadoFacturaRecepcion {
    PENDIENTE_DECISION,      // XML subido, esperando que usuario decida
    ACEPTADA,                // Usuario aceptó, mensaje enviado a Hacienda
    RECHAZADA,               // Usuario rechazó, mensaje enviado a Hacienda
    ACEPTADA_PARCIAL,        // Usuario aceptó parcialmente
    CONVERTIDA_COMPRA,       // Ya se convirtió en registro de Compra
    ERROR_HACIENDA,          // Error al validar/enviar a Hacienda
    FACTURA_RECHAZADA_MH     // Hacienda la rechazó (no es válida)
}