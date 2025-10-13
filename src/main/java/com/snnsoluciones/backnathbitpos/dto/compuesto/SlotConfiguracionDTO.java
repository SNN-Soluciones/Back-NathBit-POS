package com.snnsoluciones.backnathbitpos.dto.compuesto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO que representa un slot dentro de una configuración
 * Incluye los valores base del slot y los overrides específicos de la configuración
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
    
    // Info adicional del slot
    private Boolean usaFamilia;
    private Long familiaId;
    private String familiaNombre;
    
    /**
     * Devuelve el valor efectivo de cantidadMinima
     * (usa override si existe, sino el original)
     */
    public Integer getCantidadMinimaEfectiva() {
        return cantidadMinimaOverride != null ? cantidadMinimaOverride : cantidadMinimaOriginal;
    }
    
    /**
     * Devuelve el valor efectivo de cantidadMaxima
     */
    public Integer getCantidadMaximaEfectiva() {
        return cantidadMaximaOverride != null ? cantidadMaximaOverride : cantidadMaximaOriginal;
    }
    
    /**
     * Devuelve el valor efectivo de esRequerido
     */
    public Boolean getEsRequeridoEfectivo() {
        return esRequeridoOverride != null ? esRequeridoOverride : esRequeridoOriginal;
    }
    
    /**
     * Devuelve el valor efectivo de precioAdicional
     */
    public BigDecimal getPrecioAdicionalEfectivo() {
        return precioAdicionalOverride != null ? precioAdicionalOverride : precioAdicionalOriginal;
    }
    
    /**
     * Indica si este slot tiene algún override activo
     */
    public Boolean getTieneOverrides() {
        return cantidadMinimaOverride != null 
            || cantidadMaximaOverride != null 
            || esRequeridoOverride != null 
            || precioAdicionalOverride != null;
    }
}