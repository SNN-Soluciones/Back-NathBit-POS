package com.snnsoluciones.backnathbitpos.enums.mh;

public enum EstadoCompra {
    BORRADOR,           // En proceso de creación
    PENDIENTE_ENVIO,    // Lista para enviar a Hacienda
    ENVIADA,            // Enviada a Hacienda
    ACEPTADA,           // Aceptada por Hacienda
    RECHAZADA,          // Rechazada por Hacienda
    ERROR,              // Error en el proceso
    ANULADA             // Anulada
}