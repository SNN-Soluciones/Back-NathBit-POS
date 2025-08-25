package com.snnsoluciones.backnathbitpos.integrations.hacienda.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecepcionRequest {
    private String clave;                      // clave de 50
    private String fecha;                      // ISO-8601 con offset -0600
    private IdentificacionDTO emisor;          // requerido
    private IdentificacionDTO receptor;        // opcional
    private String comprobanteXml;             // XML firmado en base64 (UTF-8)
    private String callbackUrl;                // opcional
}