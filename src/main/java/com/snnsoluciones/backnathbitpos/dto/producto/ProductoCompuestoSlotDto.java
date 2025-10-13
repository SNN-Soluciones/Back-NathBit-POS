package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para ProductoCompuestoSlot
 * Incluye soporte para familias dinámicas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoCompuestoSlotDto {

    private Long id;
    private String nombre;
    private String descripcion;
    private Integer cantidadMinima;
    private Integer cantidadMaxima;
    private Boolean esRequerido;
    private Integer orden;

    /**
     * Opciones manuales del slot (cuando usa_familia = false)
     */
    private List<ProductoCompuestoOpcionDto> opciones;

    // ========== NUEVOS CAMPOS PARA FAMILIAS ==========

    /**
     * ID de la familia asociada (nullable)
     */
    private Long familiaId;

    /**
     * Nombre de la familia (para mostrar en UI)
     */
    private String familiaNombre;

    /**
     * Código de la familia (opcional)
     */
    private String familiaCodigo;

    /**
     * Indica si usa familia (true) u opciones manuales (false)
     */
    private Boolean usaFamilia;

    /**
     * Precio adicional por opción (cuando usa familia)
     * Se suma a cada producto de la familia
     */
    private BigDecimal precioAdicionalPorOpcion;

    // ========== FIN NUEVOS CAMPOS ==========
}