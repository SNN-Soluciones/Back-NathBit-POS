// src/main/java/com/snnsoluciones/backnathbitpos/entity/HistorialPago.java
package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_pagos",
    indexes = {
        @Index(name = "idx_historial_sucursal", columnList = "sucursal_id"),
        @Index(name = "idx_historial_empresa", columnList = "empresa_id"),
        @Index(name = "idx_historial_fecha", columnList = "fecha_pago"),
        @Index(name = "idx_historial_plan", columnList = "plan_pago_id")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"planPago", "sucursal", "empresa", "usuario"})
public class HistorialPago {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_pago_id", nullable = false)
    private PlanPago planPago;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;
    
    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;
    
    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;
    
    @Column(name = "periodo_inicio", nullable = false)
    private LocalDate periodoInicio;
    
    @Column(name = "periodo_fin", nullable = false)
    private LocalDate periodoFin;
    
    @Column(name = "metodo_pago", length = 50)
    private String metodoPago;
    
    @Column(name = "comprobante", length = 500)
    private String comprobante;
    
    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por")
    private Usuario usuario;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}