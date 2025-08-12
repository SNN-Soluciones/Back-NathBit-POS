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
import java.math.BigDecimal;
import java.util.Objects;
import lombok.Data;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

@Data
@Entity
@Table(name = "factura_detalles")
@ToString(exclude = {"factura", "producto"})
public class FacturaDetalle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;
    
    @Column(name = "numero_linea", nullable = false)
    private Integer numeroLinea;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
    
    @Column(nullable = false, precision = 16, scale = 3)
    private BigDecimal cantidad;
    
    @Column(name = "unidad_medida", length = 15, nullable = false)
    private String unidadMedida = "Unid";
    
    @Column(name = "precio_unitario", nullable = false, precision = 18, scale = 5)
    private BigDecimal precioUnitario;
    
    @Column(name = "monto_descuento", nullable = false, precision = 18, scale = 5)
    private BigDecimal montoDescuento = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 18, scale = 5)
    private BigDecimal subtotal;
    
    @Column(nullable = false, precision = 18, scale = 5)
    private BigDecimal impuesto;
    
    @Column(nullable = false, precision = 18, scale = 5)
    private BigDecimal total;
    
    // Campos para facturación electrónica
    @Column(name = "codigo_cabys", length = 13)
    private String codigoCabys;
    
    @Column(name = "detalle", length = 200)
    private String detalle;
    
    // Cálculo automático
    @PrePersist
    @PreUpdate
    public void calcularTotales() {
        if (cantidad != null && precioUnitario != null) {
            subtotal = cantidad.multiply(precioUnitario).subtract(montoDescuento);
            // Por ahora asumimos IVA 13%
            impuesto = subtotal.multiply(new BigDecimal("0.13"));
            total = subtotal.add(impuesto);
        }
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
        FacturaDetalle that = (FacturaDetalle) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass().hashCode() : getClass().hashCode();
    }
}