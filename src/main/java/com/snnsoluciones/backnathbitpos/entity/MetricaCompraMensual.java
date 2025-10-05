package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Métricas agregadas de compras por mes
 * Se actualiza automáticamente cuando se procesa una compra
 */
@Entity
@Table(name = "metricas_compras_mensuales",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_metrica_compra_mes",
            columnNames = {"empresa_id", "sucursal_id", "anio", "mes"}
        )
    },
    indexes = {
        @Index(name = "idx_metrica_compra_empresa", columnList = "empresa_id"),
        @Index(name = "idx_metrica_compra_sucursal", columnList = "sucursal_id"),
        @Index(name = "idx_metrica_compra_periodo", columnList = "anio, mes")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"empresa", "sucursal"})
@ToString(exclude = {"empresa", "sucursal"})
public class MetricaCompraMensual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    // ==================== PERIODO ====================

    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Column(name = "mes", nullable = false)
    private Integer mes;

    // ==================== MÉTRICAS GENERALES ====================

    /**
     * Total de compras realizadas en el mes
     */
    @Column(name = "total_compras", nullable = false)
    @Builder.Default
    private Long totalCompras = 0L;

    /**
     * Monto total de todas las compras
     */
    @Column(name = "monto_total", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal montoTotal = BigDecimal.ZERO;

    /**
     * Monto total de impuestos pagados
     */
    @Column(name = "monto_total_impuestos", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal montoTotalImpuestos = BigDecimal.ZERO;

    /**
     * Cantidad de proveedores únicos en el mes
     */
    @Column(name = "cantidad_proveedores", nullable = false)
    @Builder.Default
    private Integer cantidadProveedores = 0;

    // ==================== TOP PROVEEDOR ====================

    /**
     * ID del proveedor con mayor monto de compras
     */
    @Column(name = "top_proveedor_id")
    private Long topProveedorId;

    /**
     * Monto total del top proveedor
     */
    @Column(name = "top_proveedor_monto", precision = 18, scale = 2)
    private BigDecimal topProveedorMonto;

    // ==================== TOP PRODUCTO (por CABYS) ====================

    /**
     * Código CABYS del producto más comprado
     */
    @Column(name = "top_producto_cabys", length = 13)
    private String topProductoCabys;

    /**
     * Monto total del top producto
     */
    @Column(name = "top_producto_monto", precision = 18, scale = 2)
    private BigDecimal topProductoMonto;

    // ==================== TIMESTAMPS ====================

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}