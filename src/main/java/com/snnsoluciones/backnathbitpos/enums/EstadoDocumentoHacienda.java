package com.snnsoluciones.backnathbitpos.enums;

public enum EstadoDocumentoHacienda {
    PENDIENTE,          // Pendiente de enviar
    ENVIADO,           // Enviado, esperando respuesta
    PROCESANDO,        // Hacienda procesando
    ACEPTADO,          // Aceptado por Hacienda
    ACEPTADO_PARCIAL,  // Aceptado con advertencias
    RECHAZADO,         // Rechazado por Hacienda
    ERROR              // Error en el envío
}