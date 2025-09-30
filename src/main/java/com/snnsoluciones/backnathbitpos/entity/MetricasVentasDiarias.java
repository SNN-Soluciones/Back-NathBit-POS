package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "metricas_ventas_diarias",
    indexes = {
        @Index(name = "idx_mvd_fecha", columnList = "fecha"),
        @Index(name = "idx_mvd_empresa", columnList = "empresa_id"),
        @Index(name = "idx_mvd_emp_suc_fecha", columnList = "empresa_id, sucursal_id, fecha")
    },
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"empresa_id", "sucursal_key", "fecha"})
    }
)
@Check(constraints =
    "ventas_mh >= 0 AND ventas_internas >= 0 AND impuesto_total >= 0 " +
        "AND descuentos_total >= 0 AND cantidad_mh >= 0 AND cantidad_internas >= 0"
)
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class MetricasVentasDiarias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal; // NULL = consolidado

    /**
     * Columna generada en DB (COALESCE(sucursal_id, -1))
     * No se maneja en Java, solo reflejo en la tabla.
     */
    @Column(name = "sucursal_key", insertable = false, updatable = false)
    private Integer sucursalKey;

    @Column(nullable = false)
    private LocalDate fecha;

    // Montos
    @Builder.Default
    @Column(name = "ventas_mh", nullable = false, precision = 15, scale = 2)
    private BigDecimal ventasMh = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "ventas_internas", nullable = false, precision = 15, scale = 2)
    private BigDecimal ventasInternas = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "impuesto_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal impuestoTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "descuentos_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal descuentosTotal = BigDecimal.ZERO;

    // Contadores
    @Builder.Default
    @Column(name = "cantidad_mh", nullable = false)
    private Integer cantidadMh = 0;

    @Builder.Default
    @Column(name = "cantidad_internas", nullable = false)
    private Integer cantidadInternas = 0;

    // Auditoría
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Helpers
    public BigDecimal getVentasTotales() {
        return ventasMh.add(ventasInternas);
    }

    public Integer getCantidadTotal() {
        return cantidadMh + cantidadInternas;
    }
}