package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Producto específico incluido en el alcance de una promoción.
 * Tabla: tenant_X.promocion_productos
 *
 * Si esta tabla tiene registros para una promo, la promo aplica
 * SOLO a esos productos puntuales (además de los de familia/categoría).
 *
 * Diferencia con PromocionItem:
 *   - PromocionItem  → define REGLAS de ronda (ALL_YOU_CAN_EAT, BARRA_LIBRE)
 *   - PromocionProducto → define ALCANCE (¿a qué productos aplica la promo?)
 *
 * producto_id sin FK explícita, igual que el resto del sistema.
 * nombre_producto desnormalizado para evitar JOINs en lectura.
 */
@Entity
@Table(
    name = "promocion_productos",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_promocion_producto_alcance",
        columnNames = {"promocion_id", "producto_id"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromocionProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promocion_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Promocion promocion;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "nombre_producto", nullable = false, length = 200)
    private String nombreProducto;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}