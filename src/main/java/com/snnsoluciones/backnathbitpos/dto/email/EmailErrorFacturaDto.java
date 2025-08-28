package com.snnsoluciones.backnathbitpos.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para envío de notificaciones de error de factura
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailErrorFacturaDto {
    
    private Long facturaId;
    private String clave;
    private String consecutivo;
    private String emailDestino;
    private String mensajeError;
    private String codigoError;
    private byte[] xmlRespuestaBytes;
    private byte[] pdfBytes; // Si se pudo generar
    
    // Información adicional para el template
    private String nombreCliente;
    private String nombreEmpresa;
    private String cedulaEmpresa;
    private String fechaEmision;
    private String montoTotal;
}