package com.snnsoluciones.backnathbitpos.dto.producto;

import com.snnsoluciones.backnathbitpos.enums.TipoInventario;
import com.snnsoluciones.backnathbitpos.enums.mh.Moneda;
import com.snnsoluciones.backnathbitpos.enums.mh.UnidadMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * DTO para actualizar un producto existente.
 * 
 * REGLAS:
 * - NO se puede cambiar empresaId
 * - NO se puede cambiar sucursalId
 * - NO se puede cambiar codigoInterno (es único e inmutable)
 * - Todos los campos son opcionales (solo se actualizan los que vienen)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para actualizar un producto existente")
public class ProductoUpdateDto {

    // ==================== IDENTIFICACIÓN ====================

    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    @Schema(description = "Nombre del producto", example = "Hamburguesa Premium")
    private String nombre;

    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    @Schema(description = "Descripción del producto", example = "Hamburguesa premium con queso suizo")
    private String descripcion;

    @Size(max = 50, message = "El código de barras no puede exceder 50 caracteres")
    @Schema(description = "Código de barras del producto", example = "7501234567890")
    private String codigoBarras;

    // ==================== CLASIFICACIÓN ====================

    @Schema(description = "Tipo de inventario", 
            example = "LOTES",
            allowableValues = {"SIMPLE", "LOTES", "REFERENCIA", "NINGUNO"})
    private TipoInventario tipoInventario;

    @Schema(description = "Tipo de producto",
        example = "COMPUESTO",
        allowableValues = {"VENTA", "MATERIA_PRIMA", "COMBO", "COMPUESTO", "MIXTO"})
    private String tipo;

    @Schema(description = "IDs de categorías a las que pertenece el producto", 
            example = "[1, 5, 8]")
    private Set<Long> categoriaIds;

    @Schema(description = "ID de la familia de productos", example = "3")
    private Long familiaId;

    @Schema(description = "Si true, quita la familia del producto (familiaId se ignora)", example = "false")
    private Boolean removerFamilia;

    // ==================== CABYS (HACIENDA CR) ====================

    @Schema(description = "ID del registro empresa-CABYS", example = "42")
    private Long empresaCabysId;

    // ==================== PRECIOS ====================

    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    @Schema(description = "Precio de venta del producto", example = "4200.00")
    private BigDecimal precioVenta;

    @Schema(description = "Unidad de medida", example = "UNIDAD")
    private UnidadMedida unidadMedida;

    @Schema(description = "Moneda del precio", example = "CRC")
    private Moneda moneda;

    @Schema(description = "Si el precio incluye IVA", example = "true")
    private Boolean incluyeIVA;

    @Schema(description = "Si es un servicio (afecta facturación)", example = "false")
    private Boolean esServicio;

    // ==================== IMPUESTOS ====================

    @Schema(description = "Lista de impuestos del producto (reemplaza los existentes)")
    private List<CrearImpuestoDto> impuestos;

    // ==================== INVENTARIO ====================

    @DecimalMin(value = "0.0", message = "El factor de conversión debe ser positivo")
    @Schema(description = "Factor de conversión entre unidad de venta y receta", example = "1000.0")
    private BigDecimal factorConversionReceta;

    // ==================== PRODUCTO COMPUESTO ====================

    @Schema(description = "Indica si el producto requiere personalización al venderlo", example = "true")
    private Boolean requierePersonalizacion;

    @Schema(description = "Precio base para productos compuestos", example = "2500.00")
    private BigDecimal precioBase;

    // ==================== ESTADO ====================

    @Schema(description = "Si el producto está activo", example = "true")
    private Boolean activo;
}