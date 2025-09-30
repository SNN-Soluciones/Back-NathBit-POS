package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "factura_recepcion_impuesto_total",
       uniqueConstraints = @UniqueConstraint(columnNames = {"factura_recepcion_id", "codigo", "codigo_tarifa_iva"}))
@Data
@EqualsAndHashCode(exclude = {"facturaRecepcion"})
@ToString(exclude = {"facturaRecepcion"})
public class FacturaRecepcionImpuestoTotal {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_recepcion_id", nullable = false)
    private FacturaRecepcionAutomatica facturaRecepcion;
    
    @Column(length = 2, nullable = false)
    private String codigo;
    
    @Column(name = "codigo_tarifa_iva", length = 2)
    private String codigoTarifaIVA;
    
    @Column(precision = 5, scale = 2)
    private BigDecimal tarifa;
    
    @Column(name = "factor_iva", precision = 5, scale = 2)
    private BigDecimal factorIVA;
    
    @Column(name = "total_monto_impuesto", precision = 18, scale = 5, nullable = false)
    private BigDecimal totalMontoImpuesto;
    
    @Column(name = "total_monto_exoneracion", precision = 18, scale = 5)
    private BigDecimal totalMontoExoneracion = BigDecimal.ZERO;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}