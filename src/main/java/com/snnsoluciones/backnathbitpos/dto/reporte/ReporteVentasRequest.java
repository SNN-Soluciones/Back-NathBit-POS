package com.snnsoluciones.backnathbitpos.dto.reporte;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Request para generar reportes de ventas
 * Arquitectura La Jachuda 🚀
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteVentasRequest {
    
    @NotNull(message = "La fecha desde es requerida")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaDesde;
    
    @NotNull(message = "La fecha hasta es requerida")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaHasta;
    
    @NotNull(message = "La sucursal es requerida")
    private Long sucursalId;
    
    // Opcional: para filtrar por empresa (ROOT puede ver todas)
    private Long empresaId;
    
    // Formato de salida
    @Builder.Default
    private FormatoReporte formato = FormatoReporte.EXCEL;
    
    // Para reportes futuros más específicos
    private String actividadEconomica;
    private Boolean soloExoneradas;
    private Boolean soloGravadas;
    
    public enum FormatoReporte {
        EXCEL, PDF, CSV
    }
}