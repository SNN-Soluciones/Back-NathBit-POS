package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una configuración condicional de slots para un producto compuesto.
 * Una configuración se activa cuando se selecciona una opción específica (trigger).
 * 
 * Ejemplo:
 * - Producto: "Hamburguesa"
 * - Slot "Tamaño": ["Sencillo", "Combo"]
 * - Si elige "Sencillo" -> activa "Configuración Básica" (solo proteína)
 * - Si elige "Combo" -> activa "Configuración Completa" (proteína + papa + bebida)
 */
@Entity
@Table(name = "producto_compuesto_configuracion",
    indexes = {
        @Index(name = "idx_config_compuesto", columnList = "compuesto_id"),
        @Index(name = "idx_config_trigger", columnList = "opcion_trigger_id"),
        @Index(name = "idx_config_activa", columnList = "activa")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"compuesto", "opcionTrigger", "slots"})
public class ProductoCompuestoConfiguracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Producto compuesto al que pertenece esta configuración
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compuesto_id", nullable = false)
    private ProductoCompuesto compuesto;

    /**
     * Nombre descriptivo de la configuración
     * Ej: "Configuración Sencillo", "Configuración Combo Grande"
     */
    @Column(nullable = false, length = 100)
    private String nombre;

    /**
     * Descripción adicional (opcional)
     * Ej: "Configuración activada al elegir tamaño sencillo"
     */
    @Column(length = 255)
    private String descripcion;

    /**
     * Opción que ACTIVA esta configuración
     * Cuando el cliente selecciona esta opción, se activa esta configuración
     * y se muestran solo los slots asociados a ella
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opcion_trigger_id")
    private ProductoCompuestoOpcion opcionTrigger;

    /**
     * Orden de evaluación (por si hay múltiples configs)
     * Menor número = mayor prioridad
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;

    /**
     * Indica si la configuración está activa
     * Útil para desactivar temporalmente sin eliminar
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    /**
     * Indica si esta es la configuración por defecto
     * Solo puede haber UNA configuración default por producto compuesto
     * Se usa cuando NO hay pregunta inicial
     */
    @Column(name = "es_default", nullable = false)
    @Builder.Default
    private Boolean esDefault = false;

    /**
     * Slots asociados a esta configuración
     * Define qué slots se muestran y con qué reglas
     */
    @BatchSize(size = 15)
    @OneToMany(mappedBy = "configuracion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<ProductoCompuestoSlotConfiguracion> slots = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ==================== MÉTODOS HELPER ====================

    /**
     * Agrega un slot a esta configuración
     */
    public void agregarSlot(ProductoCompuestoSlotConfiguracion slotConfig) {
        slots.add(slotConfig);
        slotConfig.setConfiguracion(this);
    }

    /**
     * Remueve un slot de esta configuración
     */
    public void removerSlot(ProductoCompuestoSlotConfiguracion slotConfig) {
        slots.remove(slotConfig);
        slotConfig.setConfiguracion(null);
    }

    /**
     * Verifica si esta configuración debe activarse para una opción dada
     */
    public boolean seActivaConOpcion(Long opcionId) {
        return this.opcionTrigger != null && 
               this.opcionTrigger.getId().equals(opcionId);
    }

    /**
     * Valida la consistencia entre esDefault y opcionTrigger
     */
    public boolean esConfiguracionValida() {
        if (Boolean.TRUE.equals(esDefault)) {
            return opcionTrigger == null;
        } else {
            return opcionTrigger != null;
        }
    }

    /**
     * Obtiene el número de slots en esta configuración
     */
    public int getCantidadSlots() {
        return slots != null ? slots.size() : 0;
    }
}