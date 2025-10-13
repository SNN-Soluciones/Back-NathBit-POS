package com.snnsoluciones.backnathbitpos.dto.compuesto;

import com.snnsoluciones.backnathbitpos.dto.slots.OpcionSlotDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO que representa un slot dentro de una configuración
 * Incluye los valores base del slot, los overrides específicos de la configuración
 * Y LAS OPCIONES CARGADAS DINÁMICAMENTE con stock
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotConfiguracionDTO {

    private Long id; // ID del ProductoCompuestoSlotConfiguracion

    // Datos del slot base
    private Long slotId;
    private String slotNombre;
    private String slotDescripcion;
    private Integer orden;

    // Valores originales del slot
    private Integer cantidadMinimaOriginal;
    private Integer cantidadMaximaOriginal;
    private Boolean esRequeridoOriginal;
    private BigDecimal precioAdicionalOriginal;

    // Overrides específicos de esta configuración (nullable)
    private Integer cantidadMinimaOverride;
    private Integer cantidadMaximaOverride;
    private Boolean esRequeridoOverride;
    private BigDecimal precioAdicionalOverride;

    // ⭐ VALORES EFECTIVOS (Ya calculados, listos para usar)
    private Integer cantidadMinima;
    private Integer cantidadMaxima;
    private Boolean esRequerido;
    private BigDecimal precioAdicional;

    // Info adicional del slot
    private Boolean usaFamilia;
    private Long familiaId;
    private String familiaNombre;

    // ⭐ OPCIONES CARGADAS DINÁMICAMENTE CON STOCK
    private List<OpcionSlotDTO> opciones;

    /**
     * Indica si este slot tiene algún override activo
     */
    public Boolean getTieneOverrides() {
        return cantidadMinimaOverride != null
            || cantidadMaximaOverride != null
            || esRequeridoOverride != null
            || precioAdicionalOverride != null;
    }

    /**
     * Devuelve el valor efectivo de cantidadMinima
     * (usa override si existe, sino el original)
     */
    public Integer getCantidadMinimaEfectiva() {
        return cantidadMinima != null ? cantidadMinima :
            (cantidadMinimaOverride != null ? cantidadMinimaOverride : cantidadMinimaOriginal);
    }

    /**
     * Devuelve el valor efectivo de cantidadMaxima
     */
    public Integer getCantidadMaximaEfectiva() {
        return cantidadMaxima != null ? cantidadMaxima :
            (cantidadMaximaOverride != null ? cantidadMaximaOverride : cantidadMaximaOriginal);
    }

    /**
     * Devuelve el valor efectivo de esRequerido
     */
    public Boolean getEsRequeridoEfectivo() {
        return esRequerido != null ? esRequerido :
            (esRequeridoOverride != null ? esRequeridoOverride : esRequeridoOriginal);
    }

    /**
     * Devuelve el valor efectivo de precioAdicional
     */
    public BigDecimal getPrecioAdicionalEfectivo() {
        return precioAdicional != null ? precioAdicional :
            (precioAdicionalOverride != null ? precioAdicionalOverride : precioAdicionalOriginal);
    }
}