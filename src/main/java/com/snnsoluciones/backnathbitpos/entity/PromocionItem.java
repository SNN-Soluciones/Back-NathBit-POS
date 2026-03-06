package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Ítem incluido dentro de una promoción.
 * Tabla: tenant_X.promocion_items
 *
 * ─────────────────────────────────────────────────────────────────────
 * ¿CUÁNDO SE USA?
 * ─────────────────────────────────────────────────────────────────────
 * Solo aplica para promos que tienen productos definidos:
 *   - ALL_YOU_CAN_EAT  → Ej: 4 alas (ilimitadas) + 2 birras (1 sola ronda)
 *   - BARRA_LIBRE      → Lista de bebidas permitidas, consumo libre
 *   - ESPECIAL         → Cualquier combinación personalizada
 *
 * Para NXM / PORCENTAJE / MONTO_FIJO esta tabla no se usa.
 *
 * ─────────────────────────────────────────────────────────────────────
 * LÓGICA DE RONDAS
 * ─────────────────────────────────────────────────────────────────────
 * cantidad_por_ronda → Cuántas unidades se sirven por ronda.
 *                      Ejemplo: 4 alas por ronda.
 *
 * max_rondas         → Cuántas rondas puede pedir el cliente.
 *                      NULL = ilimitado (el cliente puede pedir siempre)
 *                      1    = solo una vez (ej: 2 birras, ya no más)
 *                      3    = máximo 3 rondas
 *
 * Ejemplo All You Can Eat:
 *   producto: Alitas   | cantidad_por_ronda: 4 | max_rondas: NULL  → infinitas
 *   producto: Birra    | cantidad_por_ronda: 2 | max_rondas: 1     → solo 2 en total
 *
 * ─────────────────────────────────────────────────────────────────────
 * NOTA: producto_id se guarda como Long SIN FK explícita.
 * La integridad se valida en la capa de servicio al momento de crear
 * o actualizar la promo, siguiendo el patrón del resto del sistema.
 * nombre_producto se desnormaliza para evitar JOINs en lectura.
 * ─────────────────────────────────────────────────────────────────────
 */
@Entity
@Table(name = "promocion_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromocionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Relación con Promocion ────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promocion_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Promocion promocion;

    // ── Referencia al producto (sin FK cross-schema) ──────────────────

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    /**
     * Nombre desnormalizado del producto.
     * Se copia al crear el ítem para evitar JOINs en lectura.
     * Se puede actualizar manualmente si el nombre del producto cambia.
     */
    @Column(name = "nombre_producto", nullable = false, length = 200)
    private String nombreProducto;

    // ── Reglas de cantidad ────────────────────────────────────────────

    /**
     * Unidades que se sirven por cada ronda.
     * Ejemplo: 4 alas, 2 birras.
     */
    @Column(name = "cantidad_por_ronda", nullable = false)
    private Integer cantidadPorRonda;

    /**
     * Máximo de rondas permitidas para este ítem.
     * NULL = sin límite (el cliente puede pedir cuantas quiera).
     * 1    = solo una ronda en toda la promo.
     */
    @Column(name = "max_rondas")
    private Integer maxRondas;

    // ── Auditoría ─────────────────────────────────────────────────────

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}