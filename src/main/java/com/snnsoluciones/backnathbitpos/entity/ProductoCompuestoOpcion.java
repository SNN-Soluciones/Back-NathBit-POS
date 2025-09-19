package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.TipoInventario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa una opción disponible dentro de un slot de personalización
 * Ej: "Papa Francesa", "Pollo", "Salsa Ranch"
 */
@Entity
@Table(name = "producto_compuesto_opcion",
    indexes = {
        @Index(name = "idx_slot_opciones", columnList = "slot_id"),
        @Index(name = "idx_opcion_producto", columnList = "producto_id")
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
     * Debe ser un producto existente (puede ser MIXTO o MATERIA_PRIMA)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

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
     * Se puede desactivar temporalmente sin eliminar
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

    // Métodos helper

    /**
     * Verifica si tiene cargo adicional
     */
    public boolean tieneCargoAdicional() {
        return precioAdicional != null && precioAdicional.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Obtiene el nombre para mostrar
     */
    public String getNombreDisplay() {
        if (producto == null) return "";
        
        String nombre = producto.getNombre();
        if (tieneCargoAdicional()) {
            nombre += " (+" + precioAdicional + ")";
        }
        return nombre;
    }

    /**
     * Verifica si el producto tiene stock disponible
     */
    public boolean tieneStockDisponible(Long sucursalId) {
        if (producto == null) return false;
        
        // Si el producto no requiere inventario, siempre está disponible
        if (producto.getTipoInventario() == TipoInventario.NINGUNO) {
            return true;
        }
        
        // TODO: Verificar stock real en la sucursal
        return true;
    }

    /**
     * Calcula el precio total de esta opción
     */
    public BigDecimal calcularPrecio() {
        return precioAdicional != null ? precioAdicional : BigDecimal.ZERO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductoCompuestoOpcion)) return false;
        ProductoCompuestoOpcion that = (ProductoCompuestoOpcion) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}