package com.snnsoluciones.backnathbitpos.integrations.hacienda.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ConsultaEstadoResponse {
    private String clave;

    @JsonProperty("ind-estado")
    private String indEstado; // "aceptado" | "rechazado" | "recibido" | "procesando"...

    @JsonProperty("respuesta-xml")
    private String respuestaXmlBase64; // puede venir null si aún no hay acuse

    @JsonProperty("detalle-mensaje")
    private String detalleMensaje; // opcional: causa de rechazo/error
}