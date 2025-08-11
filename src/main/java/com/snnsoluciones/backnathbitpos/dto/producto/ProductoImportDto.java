package com.snnsoluciones.backnathbitpos.dto.producto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para importación masiva
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoImportDto {
    private String codigoInterno;
    private String codigoBarras;
    private String nombre;
    private String descripcion;
    private String codigoCabys;
    private String categoriaNombre;
    private String unidadMedidaCodigo;
    private String monedaCodigo;
    private BigDecimal precioVenta;
    private String tarifaIvaCodigo;
    private Boolean aplicaServicio;
    private Boolean esServicio;
}
