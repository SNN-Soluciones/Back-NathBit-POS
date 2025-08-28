package com.snnsoluciones.backnathbitpos.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO con todos los datos necesarios para enviar una factura por email
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailFacturaDto {
    
    // Identificadores
    private Long facturaId;
    private String clave;
    private String consecutivo;
    
    // Destinatario
    private String emailDestino;
    
    // Datos para el asunto
    private String tipoDocumento;
    private String nombreComercial;
    
    // Datos para el cuerpo del email
    private String razonSocial;
    private String cedulaJuridica;
    private String fechaEmision;
    private String logoUrl;
    
    // Archivos adjuntos
    private byte[] pdfBytes;
    private byte[] xmlFirmadoBytes;
    private byte[] respuestaHaciendaBytes;
    
    // Asunto calculado
    public String getAsunto() {
        return String.format("%s - %s", tipoDocumento, nombreComercial);
    }
}