package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Métrica simple: solo cuenta cuántas veces se vendió cada producto por día
 */
@Entity
@Table(name = "metricas_productos_vendidos",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_metricas_productos",
            columnNames = {"fecha", "sucursal_id", "producto_id"}
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricaProductoVendido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    // 📊 SOLO ESTO - La cantidad vendida
    @Column(name = "cantidad_vendida", nullable = false, precision = 15, scale = 3)
    @Builder.Default
    private BigDecimal cantidadVendida = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Método helper para sumar cantidad
    public void agregarCantidad(BigDecimal cantidad) {
        this.cantidadVendida = this.cantidadVendida.add(cantidad);
    }
}