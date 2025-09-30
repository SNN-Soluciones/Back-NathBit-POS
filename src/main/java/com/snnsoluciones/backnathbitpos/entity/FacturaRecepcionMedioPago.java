package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "factura_recepcion_medio_pago")
@Data
@EqualsAndHashCode(exclude = {"facturaRecepcion"})
@ToString(exclude = {"facturaRecepcion"})
public class FacturaRecepcionMedioPago {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_recepcion_id", nullable = false)
    private FacturaRecepcionAutomatica facturaRecepcion;
    
    @Column(name = "tipo_medio_pago", length = 2, nullable = false)
    private String tipoMedioPago;
    
    @Column(length = 50)
    private String descripcion;
    
    @Column(precision = 18, scale = 5)
    private BigDecimal monto;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}