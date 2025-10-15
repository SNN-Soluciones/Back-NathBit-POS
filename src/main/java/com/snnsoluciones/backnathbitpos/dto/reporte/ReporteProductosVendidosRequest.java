package com.snnsoluciones.backnathbitpos.dto.reporte;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReporteProductosVendidosRequest {
    
    @NotNull(message = "La sucursal es requerida")
    private Long sucursalId;
    
    @NotNull(message = "La fecha desde es requerida")
    private LocalDate fechaDesde;
    
    @NotNull(message = "La fecha hasta es requerida")
    private LocalDate fechaHasta;
    
    // Top N productos a mostrar (default 10)
    private Integer topProductos = 10;
    
    // Agrupar por mes
    private Boolean agruparPorMes = true;
}