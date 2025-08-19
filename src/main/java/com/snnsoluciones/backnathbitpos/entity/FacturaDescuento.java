package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entidad para manejar Descuentos por línea según Hacienda 4.4
 * Permite hasta 5 descuentos por línea de detalle
 */
@Data
@Entity
@Table(name = "factura_descuentos")
@ToString(exclude = "facturaDetalle")
public class FacturaDescuento {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_detalle_id", nullable = false)
    private FacturaDetalle facturaDetalle;
    
    /**
     * Código según nota 20 de Hacienda:
     * 01 - Descuento por Regalía
     * 02 - Descuento por Regalía o Bonificaciones IVA Cobrado al Cliente
     * 03 - Descuento por Bonificación
     * 04 - Descuento por volumen
     * 05 - Descuento por Temporada (estacional)
     * 06 - Descuento promocional
     * 07 - Descuento Comercial
     * 08 - Descuento por frecuencia
     * 09 - Descuento sostenido
     * 99 - Otros descuentos
     */
    @Column(name = "codigo_descuento", length = 2, nullable = false)
    private String codigoDescuento;
    
    /**
     * Descripción cuando se usa código 99
     * Mínimo 5 caracteres, máximo 100
     */
    @Column(name = "codigo_descuento_otro", length = 100)
    private String codigoDescuentoOTRO;
    
    /**
     * Naturaleza del descuento (obligatorio para código 99)
     * Mínimo 3 caracteres, máximo 80
     */
    @Column(name = "naturaleza_descuento", length = 80)
    private String naturalezaDescuento;
    
    /**
     * Porcentaje de descuento
     * Ej: 10.00 para 10%
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal porcentaje;
    
    /**
     * Monto del descuento
     * Debe ser menor o igual al monto total de la línea
     */
    @Column(name = "monto_descuento", nullable = false, precision = 18, scale = 5)
    private BigDecimal montoDescuento;
    
    /**
     * Orden del descuento (1-5)
     * Los descuentos se aplican en cascada según este orden
     */
    @Column(nullable = false)
    private Integer orden;
    
    @PrePersist
    @PreUpdate
    public void validar() {
        // Validar orden
        if (orden < 1 || orden > 5) {
            throw new IllegalStateException(
                "El orden del descuento debe estar entre 1 y 5"
            );
        }
        
        // Si es código 99, validar campos adicionales
        if ("99".equals(codigoDescuento)) {
            if (codigoDescuentoOTRO == null || codigoDescuentoOTRO.trim().length() < 5) {
                throw new IllegalStateException(
                    "Código 99 requiere descripción de al menos 5 caracteres"
                );
            }
            if (naturalezaDescuento == null || naturalezaDescuento.trim().length() < 3) {
                throw new IllegalStateException(
                    "Código 99 requiere naturaleza del descuento de al menos 3 caracteres"
                );
            }
        }
        
        // Validar monto positivo
        if (montoDescuento == null || montoDescuento.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(
                "El monto del descuento debe ser mayor o igual a cero"
            );
        }
    }
    
    /**
     * Calcula el monto basado en el porcentaje y el monto base
     */
    public void calcularMonto(BigDecimal montoBase) {
        if (porcentaje != null && porcentaje.compareTo(BigDecimal.ZERO) > 0) {
            this.montoDescuento = montoBase
                .multiply(porcentaje)
                .divide(new BigDecimal("100"), 5, BigDecimal.ROUND_HALF_UP);
        }
    }
    
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy
            ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
            : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
                .getPersistentClass() 
            : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        FacturaDescuento that = (FacturaDescuento) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
                .getPersistentClass().hashCode()
            : getClass().hashCode();
    }
}