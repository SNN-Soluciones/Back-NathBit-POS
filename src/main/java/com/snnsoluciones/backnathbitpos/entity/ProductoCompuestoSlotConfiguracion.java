package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Representa la relación entre una configuración y un slot,
 * permitiendo sobrescribir las reglas del slot para esa configuración específica.
 * 
 * Ejemplo:
 * Slot "Bebida" normalmente:
 *   - cantidadMinima = 0
 *   - cantidadMaxima = 1
 *   - esRequerido = false
 *   - precioAdicional = 1.00
 * 
 * En "Configuración Combo":
 *   - cantidadMinimaOverride = 1 (AHORA ES OBLIGATORIO)
 *   - esRequeridoOverride = true
 *   - precioAdicionalOverride = 0.00 (GRATIS EN EL COMBO)
 * 
 * Si los overrides son NULL, se usan los valores originales del slot.
 */
@Entity
@Table(name = "producto_compuesto_slot_configuracion",
    indexes = {
        @Index(name = "idx_slot_config_configuracion", columnList = "configuracion_id"),
        @Index(name = "idx_slot_config_slot", columnList = "slot_id"),
        @Index(name = "idx_slot_config_orden", columnList = "configuracion_id, orden")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_config_slot",
            columnNames = {"configuracion_id", "slot_id"}
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"configuracion", "slot"})
public class ProductoCompuestoSlotConfiguracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Configuración a la que pertenece este slot
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "configuracion_id", nullable = false)
    private ProductoCompuestoConfiguracion configuracion;

    /**
     * Slot base que se está configurando
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private ProductoCompuestoSlot slot;

    /**
     * Orden de este slot dentro de esta configuración
     * Puede ser diferente al orden original del slot
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;

    // ==================== OVERRIDES (OPCIONALES) ====================
    // Si son NULL, se usan los valores del slot original
    // Si tienen valor, SOBRESCRIBEN los del slot

    /**
     * Override de cantidad mínima
     * Si es NULL, usa slot.cantidadMinima
     * Si tiene valor, usa este valor en lugar del slot original
     */
    @Column(name = "cantidad_minima_override")
    private Integer cantidadMinimaOverride;

    /**
     * Override de cantidad máxima
     * Si es NULL, usa slot.cantidadMaxima
     * Si tiene valor, usa este valor en lugar del slot original
     */
    @Column(name = "cantidad_maxima_override")
    private Integer cantidadMaximaOverride;

    /**
     * Override de requerido
     * Si es NULL, usa slot.esRequerido
     * Si tiene valor, usa este valor en lugar del slot original
     */
    @Column(name = "es_requerido_override")
    private Boolean esRequeridoOverride;

    /**
     * Override de precio adicional
     * Si es NULL, usa slot.precioAdicional (de las opciones)
     * Si tiene valor, SUMA este monto al precio de las opciones
     * Útil para: "En combo, todas las bebidas -$1.00"
     */
    @Column(name = "precio_adicional_override", precision = 18, scale = 5)
    private BigDecimal precioAdicionalOverride;

    // ==================== MÉTODOS HELPER ====================

    /**
     * Obtiene la cantidad mínima efectiva
     * (usa override si existe, sino el valor del slot)
     */
    public Integer getCantidadMinimaEfectiva() {
        return cantidadMinimaOverride != null 
            ? cantidadMinimaOverride 
            : (slot != null ? slot.getCantidadMinima() : 0);
    }

    /**
     * Obtiene la cantidad máxima efectiva
     */
    public Integer getCantidadMaximaEfectiva() {
        return cantidadMaximaOverride != null 
            ? cantidadMaximaOverride 
            : (slot != null ? slot.getCantidadMaxima() : 1);
    }

    /**
     * Obtiene si es requerido efectivo
     */
    public Boolean getEsRequeridoEfectivo() {
        return esRequeridoOverride != null 
            ? esRequeridoOverride 
            : (slot != null ? slot.getEsRequerido() : false);
    }

    /**
     * Obtiene el ajuste de precio efectivo
     */
    public BigDecimal getPrecioAdicionalEfectivo() {
        return precioAdicionalOverride != null 
            ? precioAdicionalOverride 
            : BigDecimal.ZERO;
    }

    /**
     * Verifica si tiene algún override activo
     */
    public boolean tieneOverrides() {
        return cantidadMinimaOverride != null 
            || cantidadMaximaOverride != null
            || esRequeridoOverride != null
            || precioAdicionalOverride != null;
    }

    /**
     * Obtiene descripción de los overrides activos
     */
    public String getDescripcionOverrides() {
        if (!tieneOverrides()) {
            return "Sin overrides";
        }

        StringBuilder desc = new StringBuilder("Overrides: ");
        if (cantidadMinimaOverride != null) {
            desc.append("Min=").append(cantidadMinimaOverride).append(" ");
        }
        if (cantidadMaximaOverride != null) {
            desc.append("Max=").append(cantidadMaximaOverride).append(" ");
        }
        if (esRequeridoOverride != null) {
            desc.append("Req=").append(esRequeridoOverride).append(" ");
        }
        if (precioAdicionalOverride != null) {
            desc.append("Precio=").append(precioAdicionalOverride).append(" ");
        }
        return desc.toString().trim();
    }
}