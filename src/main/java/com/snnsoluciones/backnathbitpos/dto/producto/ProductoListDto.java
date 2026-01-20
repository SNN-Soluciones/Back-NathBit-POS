package com.snnsoluciones.backnathbitpos.dto.producto;

import com.snnsoluciones.backnathbitpos.enums.ZonaPreparacion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO simplificado para listados de productos
 * Incluye solo la información esencial + impuestos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO simplificado para listados de productos")
public class ProductoListDto {

    @Schema(description = "ID del producto", example = "1")
    private Long id;

    @Schema(description = "ID de la empresa", example = "5")
    private Long empresaId;

    @Schema(description = "ID de la sucursal (null = global)", example = "12")
    private Long sucursalId;

    @Schema(description = "Nombre de la sucursal", example = "Sucursal Centro")
    private String sucursalNombre;

    @Schema(description = "Código interno", example = "PROD-001")
    private String codigoInterno;

    @Schema(description = "Código de barras", example = "7501234567890")
    private String codigoBarras;

    @Schema(description = "Nombre del producto", example = "Coca Cola 600ml")
    private String nombre;

    @Schema(description = "Tipo de producto", example = "VENTA")
    private String tipo;

    @Schema(description = "Precio de venta", example = "1500.00")
    private BigDecimal precioVenta;

    @Schema(description = "Precio final con impuestos", example = "1695.00")
    private BigDecimal precioFinal;

    @Schema(description = "ID de la familia", example = "3")
    private Long familiaId;

    @Schema(description = "Nombre de la familia", example = "Bebidas")
    private String familiaNombre;

    @Schema(description = "Color de la familia", example = "#3498db")
    private String familiaColor;

    @Schema(description = "URL de la imagen", example = "https://...")
    private String imagenUrl;

    @Schema(description = "URL de la miniatura", example = "https://...")
    private String thumbnailUrl;

    @Schema(description = "Si el producto está activo", example = "true")
    private Boolean activo;

    @Schema(description = "Si es un producto global (sin sucursal específica)")
    private Boolean esGlobal;

    @Schema(description = "Unidad de medida", example = "UNIDAD")
    private String unidadMedida;

    // 🆕 AGREGAR ZONA DE PREPARACIÓN
    @Schema(description = "Zona de preparación", example = "COCINA")
    private ZonaPreparacion zonaPreparacion;

    // 🆕 AGREGAR FLAG ES SERVICIO
    @Schema(description = "Si es un servicio (afecta facturación)", example = "false")
    private Boolean esServicio;

    @Schema(description = "Lista de impuestos del producto")
    private List<ProductoImpuestoDto> impuestos;

    @Schema(description = "Lista de categorías del producto")
    private List<CategoriaProductoDto> categorias;

    @Schema(description = "Información del CAByS asignado")
    private EmpresaCABySSelectDto empresaCabys;
}