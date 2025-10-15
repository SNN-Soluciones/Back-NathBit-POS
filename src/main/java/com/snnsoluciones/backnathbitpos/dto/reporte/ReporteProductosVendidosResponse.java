package com.snnsoluciones.backnathbitpos.dto.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteProductosVendidosResponse {
    
    // Metadatos
    private String empresaNombre;
    private String sucursalNombre;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    
    // Totales generales
    private BigDecimal totalProductosVendidos;
    private Integer cantidadProductosDistintos;
    
    // Top N productos del período completo
    private List<ProductoVendidoDTO> topProductos;
    
    // Desglose por mes (clave: "2025-10", valor: lista de productos)
    private Map<String, List<ProductoVendidoDTO>> productosPorMes;
    
    // Totales por mes (clave: "2025-10", valor: total vendido)
    private Map<String, BigDecimal> totalesPorMes;
}