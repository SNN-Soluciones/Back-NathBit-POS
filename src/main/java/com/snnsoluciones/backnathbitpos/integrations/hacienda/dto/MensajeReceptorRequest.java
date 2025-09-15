package com.snnsoluciones.backnathbitpos.integrations.hacienda.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Request para enviar mensaje receptor a Hacienda
 * Similar a RecepcionRequest pero para mensajes
 */
@Data
@Builder
public class MensajeReceptorRequest {
    
    @JsonProperty("clave")
    private String clave; // Clave del documento original
    
    @JsonProperty("fecha")
    private String fecha; // Formato ISO 8601: "2025-01-20T10:30:00-06:00"
    
    @JsonProperty("emisor")
    private Emisor emisor; // En este caso es el receptor original (nosotros)
    
    @JsonProperty("receptor")  
    private Receptor receptor; // En este caso es el emisor original (proveedor)
    
    @JsonProperty("consecutivoReceptor")
    private String consecutivoReceptor; // Nuestro consecutivo para el mensaje
    
    @JsonProperty("comprobanteXml")
    private String comprobanteXmlBase64; // El XML del mensaje firmado en base64
    
    @Data
    @Builder
    public static class Emisor {
        @JsonProperty("tipoIdentificacion")
        private String tipoIdentificacion;
        
        @JsonProperty("numeroIdentificacion")
        private String numeroIdentificacion;
    }
    
    @Data
    @Builder
    public static class Receptor {
        @JsonProperty("tipoIdentificacion")
        private String tipoIdentificacion;
        
        @JsonProperty("numeroIdentificacion")
        private String numeroIdentificacion;
    }
}