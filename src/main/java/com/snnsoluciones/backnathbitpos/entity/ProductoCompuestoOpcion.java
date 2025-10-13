package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.TipoInventario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa una opción disponible dentro de un slot de personalización
 *
 * ACTUALIZADO: Ahora soporta dos modos:
 * 1. Opción con producto (para slots normales)
 * 2. Opción sin producto (para slot maestro - solo trigger)
 */
@Entity
@Table(name = "producto_compuesto_opcion",
    indexes = {
        @Index(name = "idx_slot_opciones", columnList = "slot_id"),
        @Index(name = "idx_opcion_producto", columnList = "producto_id"),
        @Index(name = "idx_opcion_nombre", columnList = "nombre")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"slot", "producto"})
public class ProductoCompuestoOpcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Slot al que pertenece esta opción
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private ProductoCompuestoSlot slot;

    /**
     * Producto que representa esta opción
     * NULLABLE: Para slot maestro, no necesita producto (solo nombre)
     * NO NULL: Para slots normales, debe tener producto asociado
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = true) // ⭐ AHORA ES NULLABLE
    private Producto producto;

    /**
     * Nombre de la opción
     * - Si tiene producto: Se usa el nombre del producto (este campo puede ser null)
     * - Si NO tiene producto: Este campo es REQUERIDO (caso slot maestro)
     *
     * Ejemplos:
     * - Slot maestro "Sabor": nombre = "Melocotón", producto = null
     * - Slot normal "Tamaño": nombre = null, producto = "Fuze Tea Large"
     */
    @Column(name = "nombre", length = 100)
    private String nombre;

    /**
     * Precio adicional por esta opción
     * 0 = incluido en precio base
     * > 0 = cargo extra
     */
    @Column(name = "precio_adicional", precision = 18, scale = 5)
    @Builder.Default
    private BigDecimal precioAdicional = BigDecimal.ZERO;

    /**
     * Indica si esta es la opción por defecto del slot
     */
    @Column(name = "es_default", nullable = false)
    @Builder.Default
    private Boolean esDefault = false;

    /**
     * Indica si la opción está disponible
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean disponible = true;

    /**
     * Orden de presentación
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ==================== MÉTODOS HELPER ====================

    /**
     * Obtiene el nombre efectivo de la opción
     * - Si tiene nombre propio, usa ese
     * - Si no, usa el nombre del producto
     */
    public String getNombreEfectivo() {
        if (nombre != null && !nombre.isBlank()) {
            return nombre;
        }
        return producto != null ? producto.getNombre() : "Sin nombre";
    }

    /**
     * Verifica si es una opción de slot maestro (sin producto)
     */
    public boolean esOpcionMaestra() {
        return producto == null;
    }

    /**
     * Verifica si tiene cargo adicional
     */
    public boolean tieneCargoAdicional() {
        return precioAdicional != null && precioAdicional.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Valida que la opción sea válida antes de guardar
     */
    @PrePersist
    @PreUpdate
    private void validar() {
        // Si no tiene producto, DEBE tener nombre
        if (producto == null && (nombre == null || nombre.isBlank())) {
            throw new IllegalStateException(
                "Una opción sin producto debe tener un nombre definido (slot maestro)"
            );
        }

        // Si tiene producto, el nombre es opcional (se usa el del producto)
    }
}