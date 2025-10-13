package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una categoría de personalización en un producto compuesto
 * Ej: "Tipo de Papa", "Proteína", "Salsas"
 *
 * NUEVO: Ahora soporta dos modos de operación:
 * 1. Opciones Manuales (usaFamilia = false) - Como antes
 * 2. Opciones desde Familia (usaFamilia = true) - DINÁMICO
 */
@Entity
@Table(name = "producto_compuesto_slot",
    indexes = {
        @Index(name = "idx_compuesto_slots", columnList = "compuesto_id"),
        @Index(name = "idx_slot_familia", columnList = "familia_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"compuesto", "opciones", "familia"})
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
     * Opciones manuales del slot (cuando usa_familia = false)
     */
    @OneToMany(mappedBy = "slot", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductoCompuestoOpcion> opciones = new ArrayList<>();

    // ========== NUEVOS CAMPOS PARA FAMILIAS ==========

    /**
     * Familia de productos a usar como opciones dinámicas
     * Solo se usa cuando usa_familia = true
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "familia_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_slot_familia"))
    private FamiliaProducto familia;

    /**
     * Indica si este slot usa familia (true) u opciones manuales (false)
     * Default: false (opciones manuales)
     */
    @Column(name = "usa_familia", nullable = false)
    @Builder.Default
    private Boolean usaFamilia = false;

    /**
     * Precio adicional que se suma a CADA opción cuando usa familia
     * Ejemplo: Familia BEBIDAS, precio adicional +$1.50 por cualquier bebida
     * Si es null o 0, las opciones de la familia no tienen precio adicional
     */
    @Column(name = "precio_adicional_por_opcion", precision = 18, scale = 5)
    private BigDecimal precioAdicionalPorOpcion;

    // ========== FIN NUEVOS CAMPOS ==========

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Métodos helper

    /**
     * Agrega una opción al slot (solo para opciones manuales)
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
     * Obtiene las opciones disponibles (activas) - solo para opciones manuales
     */
    public List<ProductoCompuestoOpcion> getOpcionesDisponibles() {
        return opciones.stream()
            .filter(ProductoCompuestoOpcion::getDisponible)
            .toList();
    }

    /**
     * Verifica si tiene al menos una opción disponible (opciones manuales)
     */
    public boolean tieneOpcionesDisponibles() {
        return opciones.stream()
            .anyMatch(ProductoCompuestoOpcion::getDisponible);
    }

    /**
     * Obtiene la opción por defecto si existe (opciones manuales)
     */
    public ProductoCompuestoOpcion getOpcionDefault() {
        return opciones.stream()
            .filter(ProductoCompuestoOpcion::getEsDefault)
            .findFirst()
            .orElse(null);
    }

    // ========== NUEVOS MÉTODOS HELPER PARA FAMILIAS ==========

    /**
     * Verifica si el slot usa familia
     */
    public boolean usaFamilia() {
        return Boolean.TRUE.equals(this.usaFamilia);
    }

    /**
     * Verifica si el slot usa opciones manuales
     */
    public boolean usaOpcionesManuales() {
        return !usaFamilia();
    }

    /**
     * Obtiene el nombre de la familia (si usa familia)
     */
    public String getNombreFamilia() {
        if (usaFamilia() && familia != null) {
            return familia.getNombre();
        }
        return null;
    }

    /**
     * Verifica si tiene precio adicional
     */
    public boolean tienePrecioAdicional() {
        return precioAdicionalPorOpcion != null &&
            precioAdicionalPorOpcion.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Obtiene el precio adicional formateado
     */
    public BigDecimal getPrecioAdicionalODefault() {
        return precioAdicionalPorOpcion != null ?
            precioAdicionalPorOpcion : BigDecimal.ZERO;
    }

    // ========== FIN NUEVOS MÉTODOS ==========

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