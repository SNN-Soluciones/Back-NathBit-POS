package com.snnsoluciones.backnathbitpos.dto.factura;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para descarga de documentos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoDescargaDto {
    private String nombreArchivo;
    private String tipoArchivo; // XML, XML_FIRMADO, XML_RESPUESTA
    private String contentType;
    private byte[] contenido;
    private Long tamanio;
}