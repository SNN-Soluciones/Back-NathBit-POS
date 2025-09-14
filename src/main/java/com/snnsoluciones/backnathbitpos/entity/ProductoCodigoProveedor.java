package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Data;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

/**
 * Entidad que relaciona productos con sus códigos en diferentes proveedores
 * Un producto puede tener múltiples códigos según el proveedor
 */
@Entity
@Table(name = "producto_codigo_proveedor", 
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"proveedor_id", "codigo"})
    })
@Data
@ToString(exclude = {"producto", "proveedor"})
public class ProductoCodigoProveedor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;
    
    @Column(name = "codigo", nullable = false, length = 50)
    private String codigo;
    
    @Column(name = "descripcion_proveedor", length = 200)
    private String descripcionProveedor; // Como lo llama el proveedor
    
    @Column(name = "unidad_compra", length = 20)
    private String unidadCompra; // Caja, Docena, etc.
    
    @Column(name = "factor_conversion")
    private Integer factorConversion; // Ej: 1 caja = 12 unidades
    
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
    
    @Column(name = "observaciones")
    private String observaciones;
    
    // Auditoría
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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
        ProductoCodigoProveedor that = (ProductoCodigoProveedor) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass().hashCode() : getClass().hashCode();
    }
}