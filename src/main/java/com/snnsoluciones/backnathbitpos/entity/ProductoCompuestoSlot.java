package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;
import org.hibernate.annotations.BatchSize;
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
 * 3. Cantidad por opción (permiteCantidadPorOpcion = true) - NUEVO
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compuesto_id", nullable = false)
    private ProductoCompuesto compuesto;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "cantidad_minima", nullable = false)
    @Builder.Default
    private Integer cantidadMinima = 1;

    @Column(name = "cantidad_maxima", nullable = false)
    @Builder.Default
    private Integer cantidadMaxima = 1;

    @Column(name = "es_requerido", nullable = false)
    @Builder.Default
    private Boolean esRequerido = true;

    @Column(name = "max_opciones_diferentes")
    private Integer maxOpcionesDiferentes;

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;

    /**
     * NUEVO: Permite especificar cantidad individual por cada opción
     * true = Usuario puede decir "5 de Birria, 4 de Carne, 3 de Pollo"
     * false = Usuario solo selecciona opciones sin cantidad (comportamiento actual)
     *
     * Ejemplo uso:
     * Taquiza (12 tacos de 3 sabores):
     *   permite_cantidad_por_opcion = true
     *   cantidadMinima = 12, cantidadMaxima = 12
     *   Usuario selecciona: Birria x5, Carne x4, Pollo x3 = 12 total
     */
    @Column(name = "permite_cantidad_por_opcion", nullable = false)
    @Builder.Default
    private Boolean permiteCantidadPorOpcion = false;

    @BatchSize(size = 15)
    @OneToMany(mappedBy = "slot", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<ProductoCompuestoOpcion> opciones = new ArrayList<>();

    @Column(name = "usa_familia", nullable = false)
    @Builder.Default
    private Boolean usaFamilia = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "familia_id")
    private FamiliaProducto familia;

    @Column(name = "precio_adicional_por_opcion", precision = 18, scale = 5)
    private BigDecimal precioAdicionalPorOpcion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ==================== MÉTODOS HELPER ====================

    public void agregarOpcion(ProductoCompuestoOpcion opcion) {
        opciones.add(opcion);
        opcion.setSlot(this);
    }

    public void removerOpcion(ProductoCompuestoOpcion opcion) {
        opciones.remove(opcion);
        opcion.setSlot(null);
    }

    public boolean esCantidadValida(int cantidad) {
        if (cantidad < cantidadMinima) {
            return false;
        }
        return cantidad <= cantidadMaxima;
    }

    public List<ProductoCompuestoOpcion> getOpcionesDisponibles() {
        return opciones.stream()
            .filter(ProductoCompuestoOpcion::getDisponible)
            .toList();
    }

    public boolean tieneOpcionesDisponibles() {
        return opciones.stream()
            .anyMatch(ProductoCompuestoOpcion::getDisponible);
    }

    public ProductoCompuestoOpcion getOpcionDefault() {
        return opciones.stream()
            .filter(ProductoCompuestoOpcion::getEsDefault)
            .findFirst()
            .orElse(null);
    }

    public boolean usaFamilia() {
        return Boolean.TRUE.equals(this.usaFamilia);
    }

    public boolean usaOpcionesManuales() {
        return !usaFamilia();
    }

    /**
     * NUEVO: Verifica si permite cantidad por opción
     */
    public boolean permiteCantidadPorOpcion() {
        return Boolean.TRUE.equals(this.permiteCantidadPorOpcion);
    }

    public String getNombreFamilia() {
        if (usaFamilia() && familia != null) {
            return familia.getNombre();
        }
        return null;
    }

    public boolean tienePrecioAdicional() {
        return precioAdicionalPorOpcion != null &&
            precioAdicionalPorOpcion.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal getPrecioAdicionalODefault() {
        return precioAdicionalPorOpcion != null ?
            precioAdicionalPorOpcion : BigDecimal.ZERO;
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