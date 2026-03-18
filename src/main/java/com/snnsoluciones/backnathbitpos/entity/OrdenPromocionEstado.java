package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Rastrea el estado de una promoción AYCE o BARRA_LIBRE activa en una orden.
 *
 * Se crea UNA fila por cada (orden + promo + producto) cuando se activa un AYCE.
 * Ejemplo: promo "Pizza AYCE" con 2 productos → 2 filas en esta tabla.
 *
 * Permite:
 *   - Saber cuántas rondas lleva consumidas cada producto
 *   - Validar si puede pedir más (maxRondas null = ilimitado)
 *   - Registrar cuándo fue la última ronda
 */
@Entity
@Table(
    name = "orden_promocion_estado",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_orden_promo_producto",
        columnNames = {"orden_id", "promocion_id", "producto_id"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenPromocionEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Relación con la orden ─────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Orden orden;

    // ── Referencia a la promo y producto (sin FK cross-schema) ────────
    // Se guardan como Long para no crear dependencias duras.
    // La integridad se valida en el servicio.

    @Column(name = "promocion_id", nullable = false)
    private Long promocionId;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "nombre_producto", nullable = false, length = 200)
    private String nombreProducto;

    // ── Control de rondas ─────────────────────────────────────────────

    /**
     * Cuántas rondas se han consumido hasta ahora.
     * Arranca en 1 cuando se activa la promo (la primera ronda ya fue servida).
     */
    @Column(name = "rondas_consumidas", nullable = false)
    @Builder.Default
    private Integer rondasConsumidas = 1;

    /**
     * Límite de rondas para este producto en esta promo.
     * NULL = sin límite (el cliente puede pedir cuantas quiera).
     */
    @Column(name = "max_rondas")
    private Integer maxRondas;

    /**
     * Cuántas unidades se sirven por ronda.
     * Se copia de PromocionItem al activar para no depender del registro original.
     */
    @Column(name = "cantidad_por_ronda", nullable = false)
    private Integer cantidadPorRonda;

    @Column(name = "fecha_ultima_ronda", nullable = false)
    private LocalDateTime fechaUltimaRonda;

    // ── Auditoría ─────────────────────────────────────────────────────

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (fechaUltimaRonda == null) {
            fechaUltimaRonda = LocalDateTime.now();
        }
    }

    // ── Lógica de negocio ─────────────────────────────────────────────

    /**
     * true  → puede pedir más rondas.
     * false → ya llegó al máximo.
     *
     * maxRondas null = ilimitado → siempre true.
     */
    public boolean puedeServirRonda() {
        return maxRondas == null || rondasConsumidas < maxRondas;
    }

    /**
     * Registra una ronda consumida.
     * Llamar solo después de validar puedeServirRonda().
     */
    public void consumirRonda() {
        this.rondasConsumidas++;
        this.fechaUltimaRonda = LocalDateTime.now();
    }
}