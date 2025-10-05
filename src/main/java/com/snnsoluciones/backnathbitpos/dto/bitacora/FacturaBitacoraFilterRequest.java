package com.snnsoluciones.backnathbitpos.dto.bitacora;

import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para filtrar bitácoras con múltiples criterios
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaBitacoraFilterRequest {
    
    // Filtros básicos
    private String clave;
    private String consecutivo;
    private List<EstadoBitacora> estados;
    
    // Filtros de contexto
    private Long empresaId;
    private Long sucursalId;
    private Long clienteId;
    
    // Filtros de fecha
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fechaDesde;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fechaHasta;
    
    // Filtros de procesamiento
    private Boolean soloConError;
    private Boolean soloReintentables;
    private Integer intentosMinimos;
    private Integer intentosMaximos;
    
    // Paginación
    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer size = 20;
    @Builder.Default
    private String sortBy = "createdAt";
    @Builder.Default
    private String sortDirection = "DESC";
}