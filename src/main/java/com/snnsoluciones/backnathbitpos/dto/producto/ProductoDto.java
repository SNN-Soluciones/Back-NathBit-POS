package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

// DTO completo para respuesta
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoDto {
    private Long id;
    private Long empresaId;
    private String codigoInterno;
    private String codigoBarras;
    private String nombre;
    private String descripcion;
    private EmpresaCABySSelectDto empresaCabys;
    private CategoriaProductoDto categoria;
    private UnidadMedidaDto unidadMedida;
    private MonedaDto moneda;
    private BigDecimal precioVenta;
    private List<ProductoImpuestoDto> impuestos;
    private BigDecimal totalImpuestos;
    private BigDecimal precioFinal;
    private Boolean aplicaServicio;
    private Boolean esServicio;
    private Boolean activo;
    private String createdAt;
    private String updatedAt;
}
