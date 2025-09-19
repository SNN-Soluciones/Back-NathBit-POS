package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Data;
import org.hibernate.proxy.HibernateProxy;

@Data
@Entity
@Table(name = "receta_ingredientes")
public class RecetaIngrediente {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "receta_id", nullable = false)
    private ProductoReceta receta;
    
    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
    
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Método helper para obtener la unidad de medida
    public String getUnidadMedida() {
        // Si el producto tiene unidad de uso, usar esa
        if (producto.getUnidadMedida() != null) {
            return producto.getUnidadMedida().getCodigo();
        }
        // Si no, usar la de compra
        return producto.getUnidadMedidaCompra();
    }
    
//    // Método para calcular cantidad en unidad de compra
//    public BigDecimal getCantidadEnUnidadCompra() {
//        if (producto.getFactorConversion() != null) {
//            return cantidad.divide(
//                producto.getFactorConversion(),
//                4,
//                BigDecimal.ROUND_HALF_UP
//            );
//        }
//        return cantidad;
//    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        Class<?> oEffectiveClass = o instanceof HibernateProxy
            ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
            : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) {
            return false;
        }
        RecetaIngrediente that = (RecetaIngrediente) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass().hashCode() : getClass().hashCode();
    }
}