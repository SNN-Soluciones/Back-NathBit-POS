package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa un producto personalizable con opciones variables
 * Ej: Papas con curry donde el cliente elige tipo de papa, proteína, salsas
 */
@Entity
@Table(name = "producto_compuesto")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"producto", "slots"})
public class ProductoCompuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Producto principal que representa este compuesto
     * Debe tener tipo = COMPUESTO
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false, unique = true)
    private Producto producto;

    /**
     * Instrucciones para personalización
     * Ej: "Seleccione su tipo de papa, proteína y hasta 3 salsas"
     */
    @Column(name = "instrucciones_personalizacion", columnDefinition = "TEXT")
    private String instruccionesPersonalizacion;

    /**
     * Tiempo extra de preparación en minutos
     * Se suma al tiempo base del producto
     */
    @Column(name = "tiempo_preparacion_extra")
    private Integer tiempoPreparacionExtra;

    /**
     * Slot que contiene la pregunta inicial (opcional)
     * Si es NULL → no hay pregunta inicial, se usa configuración default
     * Si tiene valor → ese slot se muestra primero y sus opciones activan configuraciones
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_pregunta_inicial_id")
    private ProductoCompuestoSlot slotPreguntaInicial;

    /**
     * Nivel máximo de recursión permitido para sub-pasos
     * Por defecto 2 niveles
     */
    @Column(name = "max_nivel_subpaso")
    @Builder.Default
    private Integer maxNivelSubpaso = 2;

    /**
     * Slots de personalización
     * Cada slot es una categoría de opciones (tipo papa, proteína, etc)
     */
    @OneToMany(mappedBy = "compuesto", cascade = CascadeType.ALL,
        orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<ProductoCompuestoSlot> slots = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ==================== MÉTODOS HELPER ====================

    /**
     * Agrega un slot de personalización
     */
    public void agregarSlot(ProductoCompuestoSlot slot) {
        slots.add(slot);
        slot.setCompuesto(this);
    }

    /**
     * Remueve un slot
     */
    public void removerSlot(ProductoCompuestoSlot slot) {
        slots.remove(slot);
        slot.setCompuesto(null);
    }

    /**
     * Valida que todos los slots requeridos tengan selección
     */
    public boolean validarSeleccion(List<Long> opcionesSeleccionadas) {
        // TODO: Implementar validación
        // - Verificar que cada slot requerido tenga al menos cantidad_minima opciones
        // - Verificar que no exceda cantidad_maxima
        return true;
    }

    /**
     * Obtiene el tiempo total de preparación
     */
    public Integer getTiempoPreparacionTotal() {
        Integer tiempoBase = 0; // TODO: Obtener del producto
        return tiempoBase + (tiempoPreparacionExtra != null ? tiempoPreparacionExtra : 0);
    }

    /**
     * Verifica si todas las opciones están disponibles
     */
    public boolean todasOpcionesDisponibles() {
        return slots.stream()
            .allMatch(slot -> slot.tieneOpcionesDisponibles());
    }
}