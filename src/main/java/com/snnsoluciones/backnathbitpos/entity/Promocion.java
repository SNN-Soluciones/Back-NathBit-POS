package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.nathbitbusinesscore.model.enums.TipoPromocion;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Promoción configurada por el tenant.
 * Tabla: tenant_X.promociones
 *
 * ─────────────────────────────────────────────────────────────────────
 * LÓGICA DE DÍAS / HORARIO
 * ─────────────────────────────────────────────────────────────────────
 * El "día comercial" arranca a las 04:00am y cierra a las 03:59am
 * del día siguiente.  Ejemplo: una promo de VIERNES que empieza a las
 * 10pm aplica también a las 2am del sábado.
 * Esa lógica se resuelve en el FRONTEND; aquí solo se persisten los
 * flags de días y el rango hora_inicio / hora_fin.
 *
 * ─────────────────────────────────────────────────────────────────────
 * CAMPOS POR TIPO
 * ─────────────────────────────────────────────────────────────────────
 * NXM              → lleva_n, paga_m
 * BARRA_LIBRE      → precio_promo  (+ items en promocion_items)
 * ALL_YOU_CAN_EAT  → precio_promo  (+ items con reglas en promocion_items)
 * PORCENTAJE       → porcentaje_descuento
 * MONTO_FIJO       → monto_descuento
 * HAPPY_HOUR       → cualquier combinación de los anteriores + hora_inicio/hora_fin
 * ESPECIAL         → descripcion libre, lógica en frontend
 */
@Entity
@Table(name = "promociones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promocion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Identificación ────────────────────────────────────────────────

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoPromocion tipo;

    // ── Estado ────────────────────────────────────────────────────────

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    // ── Días activos (día comercial: 04:00am → 03:59am siguiente) ────

    @Column(name = "lunes", nullable = false)
    @Builder.Default
    private Boolean lunes = false;

    @Column(name = "martes", nullable = false)
    @Builder.Default
    private Boolean martes = false;

    @Column(name = "miercoles", nullable = false)
    @Builder.Default
    private Boolean miercoles = false;

    @Column(name = "jueves", nullable = false)
    @Builder.Default
    private Boolean jueves = false;

    @Column(name = "viernes", nullable = false)
    @Builder.Default
    private Boolean viernes = false;

    @Column(name = "sabado", nullable = false)
    @Builder.Default
    private Boolean sabado = false;

    @Column(name = "domingo", nullable = false)
    @Builder.Default
    private Boolean domingo = false;

    // ── Rango horario dentro del día (opcional) ───────────────────────
    // NULL = aplica todo el día comercial completo.
    // Ejemplo happy hour: 17:00 → 19:00

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    // ── Campos para tipo NXM ──────────────────────────────────────────
    // Ejemplo 2x1: lleva_n=2, paga_m=1
    // Ejemplo 3x2: lleva_n=3, paga_m=2

    @Column(name = "lleva_n")
    private Integer llevaN;

    @Column(name = "paga_m")
    private Integer pagaM;

    // ── Campos para tipo PORCENTAJE ───────────────────────────────────

    @Column(name = "porcentaje_descuento", precision = 5, scale = 2)
    private BigDecimal porcentajeDescuento;

    // ── Campos para tipo MONTO_FIJO ───────────────────────────────────

    @Column(name = "monto_descuento", precision = 10, scale = 2)
    private BigDecimal montoDescuento;

    // ── Campos para BARRA_LIBRE y ALL_YOU_CAN_EAT ────────────────────
    // Precio fijo que paga el cliente por la promo completa

    @Column(name = "precio_promo", precision = 10, scale = 2)
    private BigDecimal precioPromo;

    // ── Items incluidos en la promo ───────────────────────────────────
    // Solo aplica para: BARRA_LIBRE, ALL_YOU_CAN_EAT, ESPECIAL
    // Para NXM / PORCENTAJE / MONTO_FIJO esta lista estará vacía

    @OneToMany(
        mappedBy = "promocion",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<PromocionItem> items = new ArrayList<>();

    @OneToMany(
        mappedBy = "promocion",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<PromocionFamilia> familias = new ArrayList<>();

    // ── Alcance por categoría ─────────────────────────────────────────
    // Vacío = sin restricción por categoría
    @OneToMany(
        mappedBy = "promocion",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<PromocionCategoria> categorias = new ArrayList<>();

    // ── Alcance por producto específico ──────────────────────────────
    // Vacío = sin restricción por producto puntual
    @OneToMany(
        mappedBy = "promocion",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<PromocionProducto> productos = new ArrayList<>();

    // ── Auditoría ─────────────────────────────────────────────────────

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}