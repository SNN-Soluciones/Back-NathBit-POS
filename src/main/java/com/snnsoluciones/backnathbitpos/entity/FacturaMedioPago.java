package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.mh.MedioPago;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.Data;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

@Data
@Entity
@Table(name = "factura_medios_pago")
@ToString(exclude = "factura")
public class FacturaMedioPago {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;
    
    @Column(name = "medio_pago", nullable = false)
    @Enumerated(EnumType.STRING)
    private MedioPago medioPago;
    
    @Column(nullable = false, precision = 18, scale = 5)
    private BigDecimal monto;
    
    @Column(length = 100)
    private String referencia; // Número de transferencia, últimos 4 dígitos tarjeta, etc.
    
    @Column(length = 50)
    private String banco; // Para transferencias o tarjetas

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plataforma_digital_id")
    private PlataformaDigitalConfig plataformaDigital;

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
        FacturaMedioPago that = (FacturaMedioPago) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass().hashCode() : getClass().hashCode();
    }
}