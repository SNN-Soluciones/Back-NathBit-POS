package com.snnsoluciones.backnathbitpos.dto.producto;

import com.snnsoluciones.backnathbitpos.enums.TipoInventario;
import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import com.snnsoluciones.backnathbitpos.enums.mh.Moneda;
import com.snnsoluciones.backnathbitpos.enums.mh.UnidadMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * DTO completo para respuesta de productos.
 * Usado para GET /api/v3/productos/{id}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta completa de un producto")
public class ProductoDto {

    // ==================== IDENTIFICACIÓN ====================
    
    @Schema(description = "ID del producto", example = "123")
    private Long id;

    @Schema(description = "ID de la empresa propietaria", example = "5")
    private Long empresaId;

    @Schema(description = "Nombre comercial de la empresa", example = "Restaurante El Buen Sabor")
    private String empresaNombre;

    @Schema(description = "ID de sucursal (NULL = producto global)", example = "12", nullable = true)
    private Long sucursalId;

    @Schema(description = "Nombre de la sucursal (NULL si es global)", example = "Sucursal Centro", nullable = true)
    private String sucursalNombre;

    @Schema(description = "Código interno único", example = "PROD-001")
    private String codigoInterno;

    @Schema(description = "Código de barras", example = "7501234567890", nullable = true)
    private String codigoBarras;

    @Schema(description = "Nombre del producto", example = "Hamburguesa Clásica")
    private String nombre;

    @Schema(description = "Descripción del producto", example = "Hamburguesa con queso, lechuga y tomate", nullable = true)
    private String descripcion;

    // ==================== CLASIFICACIÓN ====================

    @Schema(description = "Tipo de producto", example = "VENTA")
    private TipoProducto tipo;

    @Schema(description = "Tipo de inventario", example = "SIMPLE")
    private TipoInventario tipoInventario;

    @Schema(description = "Categorías del producto")
    private Set<CategoriaProductoDto> categorias;

    @Schema(description = "ID de la familia de productos", example = "3", nullable = true)
    private Long familiaId;

    @Schema(description = "Nombre de la familia", example = "Bebidas Gaseosas", nullable = true)
    private String familiaNombre;

    @Schema(description = "Código de la familia", example = "BEB-GAS", nullable = true)
    private String familiaCodigo;

    @Schema(description = "Color de la familia en formato hex", example = "#3498db", nullable = true)
    private String familiaColor;

    // ==================== CABYS (HACIENDA CR) ====================

    @Schema(description = "Información del código CABYS asociado", nullable = true)
    private EmpresaCABySSelectDto empresaCabys;

    // ==================== PRECIOS ====================

    @Schema(description = "Unidad de medida", example = "UNIDAD")
    private UnidadMedida unidadMedida;

    @Schema(description = "Moneda del precio", example = "CRC")
    private Moneda moneda;

    @Schema(description = "Precio de venta (sin impuestos si incluyeIVA=false)", example = "3500.00")
    private BigDecimal precioVenta;

    @Schema(description = "Precio base (para productos compuestos)", example = "2500.00", nullable = true)
    private BigDecimal precioBase;

    @Schema(description = "Total de impuestos calculados", example = "455.00")
    private BigDecimal totalImpuestos;

    @Schema(description = "Precio final (precioVenta + totalImpuestos)", example = "3955.00")
    private BigDecimal precioFinal;

    @Schema(description = "Si el precio incluye IVA", example = "true")
    private Boolean incluyeIVA;

    @Schema(description = "Si es un servicio", example = "false")
    private Boolean esServicio;

    // ==================== IMPUESTOS ====================

    @Schema(description = "Lista de impuestos aplicables al producto")
    private List<ProductoImpuestoDto> impuestos;

    // ==================== INVENTARIO ====================

    @Schema(description = "Factor de conversión receta", example = "1000.0", nullable = true)
    private BigDecimal factorConversionReceta;

    // ==================== PRODUCTO COMPUESTO ====================

    @Schema(description = "Si requiere personalización al vender", example = "false")
    private Boolean requierePersonalizacion;

    // ==================== IMÁGENES ====================

    @Schema(description = "URL pública de la imagen del producto", 
            example = "https://snn-soluciones.nyc3.digitaloceanspaces.com/NathBit-POS/5/productos/PROD-001.jpg",
            nullable = true)
    private String imagenUrl;

    @Schema(description = "URL del thumbnail (150x150) - Para implementar después", 
            example = "https://snn-soluciones.nyc3.digitaloceanspaces.com/NathBit-POS/5/productos/thumbnails/PROD-001.jpg",
            nullable = true)
    private String thumbnailUrl;

    // ==================== ESTADO ====================

    @Schema(description = "Si el producto está activo", example = "true")
    private Boolean activo;

    @Schema(description = "Fecha de creación", example = "2025-01-19T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Fecha de última actualización", example = "2025-01-19T14:45:00")
    private LocalDateTime updatedAt;
}