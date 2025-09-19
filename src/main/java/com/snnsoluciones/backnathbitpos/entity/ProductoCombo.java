package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa un combo de productos
 * Un combo es un conjunto de productos vendidos como unidad con precio especial
 */
@Entity
@Table(name = "producto_combo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"producto", "items"})
public class ProductoCombo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Producto principal que representa este combo
     * Debe tener tipo = COMBO
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false, unique = true)
    private Producto producto;

    /**
     * Precio especial del combo
     * Normalmente menor que la suma de componentes
     */
    @Column(name = "precio_combo", nullable = false, precision = 18, scale = 5)
    private BigDecimal precioCombo;

    /**
     * Ahorro respecto a comprar items por separado
     */
    @Column(precision = 18, scale = 5)
    private BigDecimal ahorro;

    /**
     * Descripción adicional del combo
     * Ej: "Incluye hamburguesa, papas medianas y bebida"
     */
    @Column(name = "descripcion_combo", columnDefinition = "TEXT")
    private String descripcionCombo;

    /**
     * Items que componen este combo
     */
    @OneToMany(mappedBy = "combo", cascade = CascadeType.ALL, 
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductoComboItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Métodos helper

    /**
     * Agrega un item al combo
     */
    public void agregarItem(ProductoComboItem item) {
        items.add(item);
        item.setCombo(this);
    }

    /**
     * Remueve un item del combo
     */
    public void removerItem(ProductoComboItem item) {
        items.remove(item);
        item.setCombo(null);
    }

    /**
     * Calcula el precio total sin descuento
     */
    public BigDecimal calcularPrecioSinDescuento() {
        return items.stream()
            .map(item -> item.getPrecioUnitarioReferencia()
                .multiply(item.getCantidad()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcula el ahorro del combo
     */
    public BigDecimal calcularAhorro() {
        BigDecimal precioNormal = calcularPrecioSinDescuento();
        return precioNormal.subtract(this.precioCombo);
    }

    /**
     * Valida si el combo tiene stock disponible
     * Depende del tipo_inventario del producto
     */
    public boolean tieneStockDisponible(Long sucursalId) {
        // Si el combo tiene inventario PROPIO, verificar su propio stock
        if (producto.getTipoInventario() == TipoInventario.PROPIO) {
            // TODO: Verificar inventario del combo
            return true;
        }
        
        // Si es REFERENCIA, verificar stock de cada componente
        // TODO: Implementar verificación de componentes
        return true;
    }
}