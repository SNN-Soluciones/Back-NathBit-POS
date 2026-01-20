package com.snnsoluciones.backnathbitpos.dto.producto;

import com.snnsoluciones.backnathbitpos.enums.TipoInventario;
import com.snnsoluciones.backnathbitpos.enums.mh.Moneda;
import com.snnsoluciones.backnathbitpos.enums.mh.UnidadMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * DTO para crear un nuevo producto.
 * Soporta creación a nivel EMPRESA (global) o SUCURSAL (local).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para crear un nuevo producto")
public class ProductoCreateDto {

    // ==================== IDENTIFICACIÓN ====================

    @NotNull(message = "El ID de empresa es obligatorio")
    @Schema(description = "ID de la empresa propietaria", example = "5", required = true)
    private Long empresaId;

    @Schema(description = "ID de sucursal (NULL = producto global, con valor = producto local)",
        example = "12", nullable = true)
    private Long sucursalId;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    @Schema(description = "Nombre del producto", example = "Hamburguesa Clásica", required = true)
    private String nombre;

    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    @Schema(description = "Descripción del producto", example = "Hamburguesa con queso, lechuga y tomate")
    private String descripcion;

    @Size(max = 50, message = "El código interno no puede exceder 50 caracteres")
    @Schema(description = "Código interno único por empresa (se genera automático si es null)",
        example = "PROD-001", nullable = true)
    private String codigoInterno;

    @Size(max = 50, message = "El código de barras no puede exceder 50 caracteres")
    @Schema(description = "Código de barras del producto", example = "7501234567890", nullable = true)
    private String codigoBarras;

    @Schema(description = "Zona de preparación para restaurantes",
        example = "COCINA",
        allowableValues = {"NINGUNA", "COCINA", "BAR", "PARRILLA", "POSTRES"},
        nullable = true)
    private String zonaPreparacion;

    // ==================== CLASIFICACIÓN ====================

    @NotBlank(message = "El tipo de producto es obligatorio")
    @Schema(description = "Tipo de producto",
        example = "VENTA",
        allowableValues = {"VENTA", "MATERIA_PRIMA", "COMBO", "COMPUESTO"},
        required = true)
    private String tipo;

    @Schema(description = "Tipo de inventario",
        example = "SIMPLE",
        allowableValues = {"SIMPLE", "LOTES", "REFERENCIA", "NINGUNO"})
    private TipoInventario tipoInventario;

    @Schema(description = "IDs de categorías a las que pertenece el producto",
        example = "[1, 5, 8]")
    private Set<Long> categoriaIds;

    @Schema(description = "ID de la familia de productos", example = "3", nullable = true)
    private Long familiaId;

    // ==================== CABYS (HACIENDA CR) ====================

    @Schema(description = "ID del registro empresa-CABYS", example = "42")
    private Long empresaCabysId;

    // ==================== PRECIOS ====================

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    @Schema(description = "Precio de venta del producto", example = "3500.00", required = true)
    private BigDecimal precioVenta;

    @Schema(description = "Unidad de medida",
        example = "UNIDAD",
        defaultValue = "UNIDAD")
    private UnidadMedida unidadMedida;

    @Schema(description = "Moneda del precio",
        example = "CRC",
        defaultValue = "CRC")
    private Moneda moneda;

    @Builder.Default
    @Schema(description = "Si el precio incluye IVA", example = "true", defaultValue = "true")
    private Boolean incluyeIVA = true;

    @Builder.Default
    @Schema(description = "Si es un servicio (afecta facturación)", example = "false", defaultValue = "false")
    private Boolean esServicio = false;

    // ==================== IMPUESTOS ====================

    @Schema(description = "Lista de impuestos del producto")
    private List<CrearImpuestoDto> impuestos;

    // ==================== INVENTARIO ====================

    @DecimalMin(value = "0.0", message = "El factor de conversión debe ser positivo")
    @Schema(description = "Factor de conversión entre unidad de venta y receta",
        example = "1000.0",
        nullable = true)
    private BigDecimal factorConversionReceta;

    // ==================== PRODUCTO COMPUESTO ====================

    @Schema(description = "Indica si el producto requiere personalización al venderlo",
        example = "true",
        defaultValue = "false")
    private Boolean requierePersonalizacion;

    @Schema(description = "Precio base para productos compuestos",
        example = "2500.00",
        nullable = true)
    private BigDecimal precioBase;

    // ==================== ESTADO ====================

    @Builder.Default
    @Schema(description = "Si el producto está activo", example = "true", defaultValue = "true")
    private Boolean activo = true;
}