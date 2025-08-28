package com.snnsoluciones.backnathbitpos.dto.factura;

import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuesta de procesamiento
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoProcesamientoDto {
    private Long bitacoraId;
    private Long facturaId;
    private String clave;
    private EstadoBitacora estado;
    private String mensaje;
    private LocalDateTime procesadoAt;
    
    // URLs para descarga
    private String xmlUrl;
    private String xmlFirmadoUrl;
    private String xmlRespuestaUrl;
}