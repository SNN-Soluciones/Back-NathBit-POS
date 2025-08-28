package com.snnsoluciones.backnathbitpos.dto.factura;

import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para consulta de bitácoras con filtros
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FiltrosBitacoraDto {
    private EstadoBitacora estado;
    private Long empresaId;
    private Long sucursalId;
    private LocalDateTime fechaDesde;
    private LocalDateTime fechaHasta;
    private String clave;
    private String numeroFactura;
    
    // Paginación
    private Integer page = 0;
    private Integer size = 20;
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";
}