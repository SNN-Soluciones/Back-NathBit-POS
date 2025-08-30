package com.snnsoluciones.backnathbitpos.dto.bitacora;

import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO detallado para ver toda la información de una bitácora
 * Incluye historial completo y archivos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaBitacoraDetailResponse {
    
    // Información de la bitácora
    private Long id;
    private Long facturaId;
    private String clave;
    private EstadoBitacora estado;
    private Integer intentos;
    private LocalDateTime proximoIntento;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Rutas de archivos
    private String rutaXml;
    private String rutaXmlFirmado;
    private String rutaXmlRespuesta;
    private String rutaPdf;
    
    // Mensajes y errores
    private String ultimoError;
    private String haciendaMensaje;
    private String haciendaDetalle;
    
    // Información de la factura
    private FacturaResumenDto factura;
    
    // Historial de cambios (opcional)
    private List<BitacoraHistorialDto> historial;
    
    // URLs para descargar archivos
    private String urlDescargaXml;
    private String urlDescargaXmlFirmado;
    private String urlDescargaXmlRespuesta;
    private String urlDescargaPdf;
}