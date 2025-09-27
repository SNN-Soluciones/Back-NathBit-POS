package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "metricas_mensuales", 
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"empresa_id", "sucursal_id", "anio", "mes"})
    },
    indexes = {
        @Index(name = "idx_metricas_empresa", columnList = "empresa_id"),
        @Index(name = "idx_metricas_sucursal", columnList = "sucursal_id"),
        @Index(name = "idx_metricas_periodo", columnList = "anio, mes")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricaMensual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal; // NULL = métricas consolidadas empresa

    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private Integer mes;

    // Montos de ventas
    @Column(name = "ventas_mh", precision = 15, scale = 2)
    private BigDecimal ventasMh = BigDecimal.ZERO;

    @Column(name = "ventas_internas", precision = 15, scale = 2)
    private BigDecimal ventasInternas = BigDecimal.ZERO;

    @Column(name = "ventas_totales", precision = 15, scale = 2)
    private BigDecimal ventasTotales = BigDecimal.ZERO;

    @Column(name = "ventas_servicios", precision = 15, scale = 2)
    private BigDecimal ventasServicios = BigDecimal.ZERO;

    @Column(name = "ventas_mercancias", precision = 15, scale = 2)
    private BigDecimal ventasMercancias = BigDecimal.ZERO;

    // Devoluciones y anulaciones
    @Column(name = "notas_credito_total", precision = 15, scale = 2)
    private BigDecimal notasCreditoTotal = BigDecimal.ZERO;

    @Column(name = "anulaciones_total", precision = 15, scale = 2)
    private BigDecimal anulacionesTotal = BigDecimal.ZERO;

    @Column(name = "descuentos_total", precision = 15, scale = 2)
    private BigDecimal descuentosTotal = BigDecimal.ZERO;

    // Impuestos por tarifa
    @Column(name = "impuesto_total", precision = 15, scale = 2)
    private BigDecimal impuestoTotal = BigDecimal.ZERO;

    @Column(name = "impuesto_iva_13", precision = 15, scale = 2)
    private BigDecimal impuestoIva13 = BigDecimal.ZERO;

    @Column(name = "impuesto_iva_4", precision = 15, scale = 2)
    private BigDecimal impuestoIva4 = BigDecimal.ZERO;

    @Column(name = "impuesto_iva_2", precision = 15, scale = 2)
    private BigDecimal impuestoIva2 = BigDecimal.ZERO;

    @Column(name = "impuesto_iva_1", precision = 15, scale = 2)
    private BigDecimal impuestoIva1 = BigDecimal.ZERO;

    // Exenciones
    @Column(name = "exento_total", precision = 15, scale = 2)
    private BigDecimal exentoTotal = BigDecimal.ZERO;

    @Column(name = "exonerado_total", precision = 15, scale = 2)
    private BigDecimal exoneradoTotal = BigDecimal.ZERO;

    // Contadores
    @Column(name = "cantidad_facturas_mh")
    private Integer cantidadFacturasMh = 0;

    @Column(name = "cantidad_facturas_internas")
    private Integer cantidadFacturasInternas = 0;

    @Column(name = "cantidad_notas_credito")
    private Integer cantidadNotasCredito = 0;

    @Column(name = "cantidad_anulaciones")
    private Integer cantidadAnulaciones = 0;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}