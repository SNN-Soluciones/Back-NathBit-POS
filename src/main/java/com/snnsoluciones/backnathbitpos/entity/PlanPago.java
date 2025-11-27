// src/main/java/com/snnsoluciones/backnathbitpos/entity/PlanPago.java
package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.EstadoPlan;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "planes_pago",
    uniqueConstraints = {
        @UniqueConstraint(name = "unique_sucursal", columnNames = "sucursal_id")
    },
    indexes = {
        @Index(name = "idx_planes_empresa", columnList = "empresa_id"),
        @Index(name = "idx_planes_estado", columnList = "estado"),
        @Index(name = "idx_planes_vencimiento", columnList = "fecha_proximo_vencimiento")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"sucursal", "empresa"})
public class PlanPago {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;
    
    @Column(name = "cuota_mensual", nullable = false, precision = 10, scale = 2)
    private BigDecimal cuotaMensual;
    
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;
    
    @Column(name = "dia_vencimiento")
    @Builder.Default
    private Integer diaVencimiento = 1;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoPlan estado = EstadoPlan.ACTIVO;
    
    @Column(name = "dias_gracia")
    @Builder.Default
    private Integer diasGracia = 3;
    
    @Column(name = "fecha_ultimo_pago")
    private LocalDate fechaUltimoPago;
    
    @Column(name = "fecha_proximo_vencimiento", nullable = false)
    private LocalDate fechaProximoVencimiento;
    
    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}