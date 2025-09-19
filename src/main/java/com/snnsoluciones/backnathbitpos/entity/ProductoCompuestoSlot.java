package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una categoría de personalización en un producto compuesto
 * Ej: "Tipo de Papa", "Proteína", "Salsas"
 */
@Entity
@Table(name = "producto_compuesto_slot",
    indexes = {
        @Index(name = "idx_compuesto_slots", columnList = "compuesto_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"compuesto", "opciones"})
public class ProductoCompuestoSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Producto compuesto al que pertenece
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compuesto_id", nullable = false)
    private ProductoCompuesto compuesto;

    /**
     * Nombre del slot
     * Ej: "Tipo de Papa", "Proteína", "Salsa"
     */
    @Column(nullable = false, length = 100)
    private String nombre;

    /**
     * Descripción adicional
     * Ej: "Seleccione el tipo de papa de su preferencia"
     */
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /**
     * Cantidad mínima de opciones a seleccionar
     */
    @Column(name = "cantidad_minima", nullable = false)
    @Builder.Default
    private Integer cantidadMinima = 1;

    /**
     * Cantidad máxima de opciones a seleccionar
     */
    @Column(name = "cantidad_maxima", nullable = false)
    @Builder.Default
    private Integer cantidadMaxima = 1;

    /**
     * Indica si este slot es obligatorio
     */
    @Column(name = "es_requerido", nullable = false)
    @Builder.Default
    private Boolean esRequerido = true;

    /**
     * Orden de presentación
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;

    /**
     * Opciones disponibles para este slot
     */
    @OneToMany(mappedBy = "slot", cascade = CascadeType.ALL, 
               orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orden ASC, id ASC")
    @Builder.Default
    private List<ProductoCompuestoOpcion> opciones = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Métodos helper

    /**
     * Agrega una opción al slot
     */
    public void agregarOpcion(ProductoCompuestoOpcion opcion) {
        opciones.add(opcion);
        opcion.setSlot(this);
    }

    /**
     * Remueve una opción
     */
    public void removerOpcion(ProductoCompuestoOpcion opcion) {
        opciones.remove(opcion);
        opcion.setSlot(null);
    }

    /**
     * Valida si una cantidad de selecciones es válida
     */
    public boolean validarCantidadSeleccionada(int cantidad) {
        if (esRequerido && cantidad < cantidadMinima) {
            return false;
        }
        return cantidad <= cantidadMaxima;
    }

    /**
     * Obtiene las opciones disponibles (activas)
     */
    public List<ProductoCompuestoOpcion> getOpcionesDisponibles() {
        return opciones.stream()
            .filter(ProductoCompuestoOpcion::getDisponible)
            .toList();
    }

    /**
     * Verifica si tiene al menos una opción disponible
     */
    public boolean tieneOpcionesDisponibles() {
        return opciones.stream()
            .anyMatch(ProductoCompuestoOpcion::getDisponible);
    }

    /**
     * Obtiene la opción por defecto si existe
     */
    public ProductoCompuestoOpcion getOpcionDefault() {
        return opciones.stream()
            .filter(ProductoCompuestoOpcion::getEsDefault)
            .findFirst()
            .orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductoCompuestoSlot)) return false;
        ProductoCompuestoSlot that = (ProductoCompuestoSlot) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}