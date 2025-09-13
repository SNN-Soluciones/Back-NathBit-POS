package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Data;
import org.hibernate.proxy.HibernateProxy;

@Data
@Entity
@Table(name = "productos_inventarios",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"producto_id", "sucursal_id"})
    }
)
public class ProductoInventario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
    
    @ManyToOne
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;
    
    @Column(name = "cantidad_actual", nullable = false, precision = 10, scale = 3)
    private BigDecimal cantidadActual = BigDecimal.ZERO;
    
    @Column(name = "cantidad_minima", nullable = false, precision = 10, scale = 3)
    private BigDecimal cantidadMinima = BigDecimal.ZERO;
    
    @Column(name = "ultima_actualizacion", nullable = false)
    private LocalDateTime ultimaActualizacion = LocalDateTime.now();

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private Boolean estado = true;
    
    // Método helper para verificar si está bajo mínimo
    public boolean isBajoMinimo() {
        return cantidadActual.compareTo(cantidadMinima) < 0;
    }
    
    // Método para actualizar cantidad
    public void actualizarCantidad(BigDecimal nuevaCantidad) {
        this.cantidadActual = nuevaCantidad;
        this.ultimaActualizacion = LocalDateTime.now();
    }
    
    // Método para ajustar cantidad (sumar o restar)
    public void ajustarCantidad(BigDecimal ajuste) {
        this.cantidadActual = this.cantidadActual.add(ajuste);
        this.ultimaActualizacion = LocalDateTime.now();
    }

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
        ProductoInventario that = (ProductoInventario) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass().hashCode() : getClass().hashCode();
    }
}