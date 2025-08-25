package com.snnsoluciones.backnathbitpos.integrations.hacienda.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Payload que Hacienda envía al callback cuando resuelve un comprobante.
 * Campos alineados con /recepcion GET y notificación asíncrona.
 */
@Data
public class HaciendaCallbackRequest {
    private String clave;

    @JsonProperty("ind-estado")
    private String indEstado; // "aceptado" | "rechazado" | "recibido" | "procesando"...

    @JsonProperty("respuesta-xml")
    private String respuestaXmlBase64; // Base64 del XML de acuse/respuesta

    @JsonProperty("detalle-mensaje")
    private String detalleMensaje; // opcional
}