package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "metricas_ventas_mensuales",
    indexes = {
        @Index(name = "idx_mvm_empresa", columnList = "empresa_id"),
        @Index(name = "idx_mvm_emp_suc_periodo", columnList = "empresa_id, sucursal_id, anio, mes")
    },
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"empresa_id", "sucursal_key", "anio", "mes"})
    }
)
@Check(constraints =
    "mes BETWEEN 1 AND 12 AND " +
        "ventas_mh >= 0 AND ventas_internas >= 0 AND ventas_totales >= 0 " +
        "AND impuesto_total >= 0 AND notas_credito_total >= 0 AND anulaciones_total >= 0 " +
        "AND descuentos_total >= 0"
)
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class MetricasVentasMensuales {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @Column(name = "sucursal_key", insertable = false, updatable = false)
    private Integer sucursalKey;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private Integer mes;

    // Montos
    @Builder.Default
    @Column(name = "ventas_mh", nullable = false, precision = 15, scale = 2)
    private BigDecimal ventasMh = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "ventas_internas", nullable = false, precision = 15, scale = 2)
    private BigDecimal ventasInternas = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "ventas_totales", nullable = false, precision = 15, scale = 2)
    private BigDecimal ventasTotales = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "ventas_servicios", nullable = false, precision = 15, scale = 2)
    private BigDecimal ventasServicios = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "ventas_mercancias", nullable = false, precision = 15, scale = 2)
    private BigDecimal ventasMercancias = BigDecimal.ZERO;

    // Ajustes
    @Builder.Default
    @Column(name = "notas_credito_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal notasCreditoTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "anulaciones_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal anulacionesTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "descuentos_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal descuentosTotal = BigDecimal.ZERO;

    // Impuestos
    @Builder.Default
    @Column(name = "impuesto_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal impuestoTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "impuesto_iva_13", nullable = false, precision = 15, scale = 2)
    private BigDecimal impuestoIva13 = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "impuesto_iva_4", nullable = false, precision = 15, scale = 2)
    private BigDecimal impuestoIva4 = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "impuesto_iva_2", nullable = false, precision = 15, scale = 2)
    private BigDecimal impuestoIva2 = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "impuesto_iva_1", nullable = false, precision = 15, scale = 2)
    private BigDecimal impuestoIva1 = BigDecimal.ZERO;

    // Exenciones
    @Builder.Default
    @Column(name = "exento_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal exentoTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "exonerado_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal exoneradoTotal = BigDecimal.ZERO;

    // Contadores
    @Builder.Default
    @Column(name = "cantidad_facturas_mh", nullable = false)
    private Integer cantidadFacturasMh = 0;

    @Builder.Default
    @Column(name = "cantidad_facturas_internas", nullable = false)
    private Integer cantidadFacturasInternas = 0;

    @Builder.Default
    @Column(name = "cantidad_notas_credito", nullable = false)
    private Integer cantidadNotasCredito = 0;

    @Builder.Default
    @Column(name = "cantidad_anulaciones", nullable = false)
    private Integer cantidadAnulaciones = 0;

    // Auditoría
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Helpers
    public BigDecimal getNetoDelMes() {
        return ventasTotales
            .subtract(descuentosTotal)
            .subtract(notasCreditoTotal)
            .subtract(anulacionesTotal);
    }
}