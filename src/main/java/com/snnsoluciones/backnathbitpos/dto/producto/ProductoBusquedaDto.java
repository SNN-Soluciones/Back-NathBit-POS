package com.snnsoluciones.backnathbitpos.dto.producto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para búsqueda rápida (POS)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoBusquedaDto {
    private Long id;
    private String codigo;
    private String nombre;
    private BigDecimal precio;
    private String moneda;
    private Boolean aplicaServicio;
}
