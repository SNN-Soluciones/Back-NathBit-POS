package com.snnsoluciones.backnathbitpos.dto.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoVendidoDTO {
    
    private Long productoId;
    private String codigoProducto;
    private String nombreProducto;
    private String categoria;
    
    // Cantidad total vendida
    private BigDecimal cantidadVendida;
    
    // Ranking (1 = más vendido)
    private Integer posicion;
    
    // Porcentaje del total
    private BigDecimal porcentaje;
}