package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ventas_diarias", 
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"empresa_id", "sucursal_id", "fecha"})
    },
    indexes = {
        @Index(name = "idx_ventas_fecha", columnList = "fecha"),
        @Index(name = "idx_ventas_empresa", columnList = "empresa_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal; // NULL = consolidado empresa

    @Column(nullable = false)
    private LocalDate fecha;

    // Montos
    @Column(name = "ventas_mh", precision = 15, scale = 2)
    private BigDecimal ventasMh = BigDecimal.ZERO;

    @Column(name = "ventas_internas", precision = 15, scale = 2)
    private BigDecimal ventasInternas = BigDecimal.ZERO;

    @Column(name = "impuesto_total", precision = 15, scale = 2)
    private BigDecimal impuestoTotal = BigDecimal.ZERO;

    @Column(name = "descuentos_total", precision = 15, scale = 2)
    private BigDecimal descuentosTotal = BigDecimal.ZERO;

    // Contadores
    @Column(name = "cantidad_mh")
    private Integer cantidadMh = 0;

    @Column(name = "cantidad_internas")
    private Integer cantidadInternas = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper method
    public BigDecimal getVentasTotales() {
        return ventasMh.add(ventasInternas);
    }

    public Integer getCantidadTotal() {
        return cantidadMh + cantidadInternas;
    }
}