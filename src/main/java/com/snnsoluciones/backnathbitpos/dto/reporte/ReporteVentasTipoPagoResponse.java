
// ReporteVentasTipoPagoResponse.java
package com.snnsoluciones.backnathbitpos.dto.reporte;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReporteVentasTipoPagoResponse {
    
    // Datos del reporte
    private String sucursalNombre;
    private String empresaNombre;
    private String empresaIdentificacion;
    private LocalDateTime fechaGeneracion;
    private String usuarioGenera;
    
    // Período
    private String fechaDesde;
    private String fechaHasta;
    
    // Datos agregados
    private List<VentasPorTipoPagoDTO> detalles;
    private BigDecimal totalGeneral;
    private Integer totalDocumentos;
    
    // Para descarga
    private byte[] archivoGenerado;
    private String nombreArchivo;
    private String tipoContenido;
}
