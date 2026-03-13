package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Rastrea el estado de consumo de una promoción por rondas dentro
 * de una orden activa.
 *
 * Tabla: orden_promocion_estados
 *
 * ─────────────────────────────────────────────────────────────────────
 * ¿CUÁNDO SE USA?
 * ─────────────────────────────────────────────────────────────────────
 * Solo aplica para:
 *   - ALL_YOU_CAN_EAT → rastrear cuántas rondas de alitas/birras ya
 *                       se sirvieron en esta orden.
 *   - BARRA_LIBRE     → igual, si tiene límite de rondas por producto.
 *
 * ─────────────────────────────────────────────────────────────────────
 * CICLO DE VIDA
 * ─────────────────────────────────────────────────────────────────────
 * 1. Mesero agrega ítem trigger (ej: "Promo Alitas AYCE") a la orden.
 * 2. Sistema crea los OrdenItem iniciales (alitas x4, birra x2) en $0.
 * 3. Sistema crea un OrdenPromocionEstado por cada producto en
 *    PromocionItem, con rondasConsumidas = 1.
 * 4. Mesero solicita nueva ronda de alitas.
 * 5. Sistema busca este registro, valida que maxRondas sea null
 *    o rondasConsumidas < maxRondas, crea nuevo OrdenItem en $0
 *    e incrementa rondasConsumidas.
 *
 * ─────────────────────────────────────────────────────────────────────
 * UNIQUE CONSTRAINT
 * ─────────────────────────────────────────────────────────────────────
 * Una orden + promo + producto = un solo registro de estado.
 * Si el mismo producto aparece en dos promos distintas en la misma
 * orden (stack), habrá dos registros separados.
 */
@Entity
@Table(
    name = "orden_promocion_estados",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_orden_promocion_producto",
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

    // ── Referencia a la promo (sin FK para mantener patrón del sistema)

    @Column(name = "promocion_id", nullable = false)
    private Long promocionId;

    // ── Producto rastreado ────────────────────────────────────────────

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "nombre_producto", nullable = false, length = 200)
    private String nombreProducto;

    // ── Estado de rondas ──────────────────────────────────────────────

    /**
     * Cuántas rondas de este producto ya se han servido en esta orden.
     * Empieza en 1 cuando se activa la promo (la ronda inicial cuenta).
     */
    @Column(name = "rondas_consumidas", nullable = false)
    @Builder.Default
    private Integer rondasConsumidas = 1;

    /**
     * Copia del max_rondas del PromocionItem al momento de activar la promo.
     * NULL = sin límite (AYCE puro).
     * Se desnormaliza aquí para no necesitar JOIN al validar cada ronda.
     */
    @Column(name = "max_rondas")
    private Integer maxRondas;

    /**
     * Unidades por ronda. Copia de PromocionItem.cantidadPorRonda.
     * Desnormalizado por la misma razón.
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
        fechaUltimaRonda = LocalDateTime.now();
    }

    // ── Métodos de utilidad ───────────────────────────────────────────

    /**
     * Verifica si aún se pueden pedir más rondas de este producto.
     * Si maxRondas es null, siempre puede (AYCE ilimitado).
     */
    public boolean puedeServirRonda() {
        return maxRondas == null || rondasConsumidas < maxRondas;
    }

    /**
     * Registra una nueva ronda consumida.
     * Llamar solo después de validar puedeServirRonda().
     */
    public void consumirRonda() {
        this.rondasConsumidas++;
        this.fechaUltimaRonda = LocalDateTime.now();
    }
}