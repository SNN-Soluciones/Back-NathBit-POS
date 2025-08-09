package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tipos_cambio",
       uniqueConstraints = @UniqueConstraint(columnNames = {"moneda_id", "fecha"}))
@ToString(exclude = {"moneda"})
public class TipoCambio {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moneda_id", nullable = false)
    private Moneda moneda;
    
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;
    
    @Column(name = "tipo_cambio_compra", precision = 10, scale = 4, nullable = false)
    private BigDecimal tipoCambioCompra;
    
    @Column(name = "tipo_cambio_venta", precision = 10, scale = 4, nullable = false)
    private BigDecimal tipoCambioVenta;
    
    // Para conversión USD-EUR cuando sea necesario
    @Column(name = "tipo_cambio_referencia", precision = 10, scale = 4)
    private BigDecimal tipoCambioReferencia;
    
    @Column(name = "fuente", length = 20)
    private String fuente; // "BCCR", "MANUAL", "API"
    
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
    
    @PrePersist
    protected void onCreate() {
        fechaActualizacion = LocalDateTime.now();
    }
}