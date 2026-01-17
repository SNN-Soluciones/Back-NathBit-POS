package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para ProductoCompuestoSlot
 * Incluye soporte para familias dinámicas y cantidad por opción
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
     * NUEVO: Permite cantidad individual por opción
     */
    private Boolean permiteCantidadPorOpcion;

    /**
     * Opciones manuales del slot (cuando usa_familia = false)
     */
    private List<ProductoCompuestoOpcionDto> opciones;

    // ========== CAMPOS PARA FAMILIAS ==========

    private Long familiaId;
    private String familiaNombre;
    private String familiaCodigo;
    private Boolean usaFamilia;
    private BigDecimal precioAdicionalPorOpcion;
}