package com.snnsoluciones.backnathbitpos.enums.mh;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EstadoMensajeReceptor {
    
    BORRADOR("Borrador", "Mensaje en construcción"),
    PENDIENTE_FIRMA("Pendiente Firma", "XML generado, pendiente firma digital"),
    PENDIENTE_ENVIO("Pendiente Envío", "Firmado, pendiente envío a Hacienda"),
    ENVIADO("Enviado", "Enviado a Hacienda exitosamente"),
    PROCESADO("Procesado", "Procesado y aceptado por Hacienda"),
    ERROR("Error", "Error en algún paso del proceso"),
    RECHAZADO_MH("Rechazado MH", "Rechazado por Ministerio de Hacienda");
    
    private final String nombre;
    private final String descripcion;
}