package com.snnsoluciones.backnathbitpos.dto.producto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para filtros de búsqueda
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoFiltroDto {
    private String busqueda;
    private Long categoriaId;
    private BigDecimal precioMin;
    private BigDecimal precioMax;
    private Boolean esServicio;
    private Boolean aplicaServicio;
    private Boolean soloActivos = true;
}