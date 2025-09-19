package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa un item dentro de un combo
 * Define qué productos y en qué cantidad forman parte del combo
 */
@Entity
@Table(name = "producto_combo_item",
    indexes = {
        @Index(name = "idx_combo_items_combo", columnList = "combo_id"),
        @Index(name = "idx_combo_items_producto", columnList = "producto_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"combo", "producto"})
public class ProductoComboItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Combo al que pertenece este item
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id", nullable = false)
    private ProductoCombo combo;

    /**
     * Producto que forma parte del combo
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    /**
     * Cantidad del producto en el combo
     * Ej: 2 hamburguesas en un combo familiar
     */
    @Column(nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal cantidad = BigDecimal.ONE;

    /**
     * Precio unitario de referencia
     * Para calcular el ahorro del combo
     */
    @Column(name = "precio_unitario_referencia", precision = 18, scale = 5)
    private BigDecimal precioUnitarioReferencia;

    /**
     * Indica si este item es opcional en el combo
     * Ej: "Sin cebolla" en una hamburguesa del combo
     */
    @Column(name = "es_opcional", nullable = false)
    @Builder.Default
    private Boolean esOpcional = false;

    /**
     * Orden de presentación en el combo
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Métodos helper

    /**
     * Calcula el precio total de este item
     */
    public BigDecimal calcularPrecioTotal() {
        if (precioUnitarioReferencia == null || cantidad == null) {
            return BigDecimal.ZERO;
        }
        return precioUnitarioReferencia.multiply(cantidad);
    }

    /**
     * Verifica si el item tiene stock disponible
     */
    public boolean tieneStockDisponible(Long sucursalId) {
        // TODO: Implementar verificación de inventario
        // Debe verificar según el tipo_inventario del producto
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductoComboItem)) return false;
        ProductoComboItem that = (ProductoComboItem) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}