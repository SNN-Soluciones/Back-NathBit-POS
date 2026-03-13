package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.CriterioDescuento;
import com.snnsoluciones.backnathbitpos.enums.CriterioItemGratis;
import com.snnsoluciones.backnathbitpos.enums.TipoPromocion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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

    // ── Vigencia por fecha (opcional) ─────────────────────────────────
    // NULL en ambos = siempre activa (solo se rige por días y horario)
    // Si vienen fechas, la promo solo aplica dentro de ese rango.

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

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

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    // ── Stacking ──────────────────────────────────────────────────────
    // Si es true, esta promo puede combinarse con otras en la misma orden.
    // El admin de la sucursal lo configura.

    @Column(name = "permitir_stack", nullable = false)
    @Builder.Default
    private Boolean permitirStack = false;

    // ── Campos para tipo NXM ──────────────────────────────────────────

    @Column(name = "lleva_n")
    private Integer llevaN;

    @Column(name = "paga_m")
    private Integer pagaM;

    /**
     * Cómo se elige el ítem gratis en NXM.
     * MAS_BARATO          → el de menor precio entre los BENEFICIO de la orden.
     * PRODUCTO_ESPECIFICO → definido en PromocionProducto con rol BENEFICIO.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "criterio_item_gratis", length = 30)
    private CriterioItemGratis criterioItemGratis;

    // ── Campos para tipo PORCENTAJE ───────────────────────────────────

    @Column(name = "porcentaje_descuento", precision = 5, scale = 2)
    private BigDecimal porcentajeDescuento;

    // ── Campos para tipo MONTO_FIJO ───────────────────────────────────

    @Column(name = "monto_descuento", precision = 10, scale = 2)
    private BigDecimal montoDescuento;

    // ── Campos para BARRA_LIBRE y ALL_YOU_CAN_EAT ────────────────────

    @Column(name = "precio_promo", precision = 10, scale = 2)
    private BigDecimal precioPromo;

    // ── Campos para tipo GRUPO_CONDICIONAL ────────────────────────────
    // Ejemplo: 2 adultos (trigger) → 1 niño gratis (beneficio)

    /**
     * Cuántos ítems del grupo TRIGGER se necesitan para activar la promo.
     * Ejemplo: 2 (dos adultos deben ordenar algo del alcance TRIGGER).
     */
    @Column(name = "cantidad_trigger")
    private Integer cantidadTrigger;

    /**
     * Cuántos ítems del grupo BENEFICIO reciben el descuento.
     * Ejemplo: 1 (un niño se beneficia).
     */
    @Column(name = "cantidad_beneficio")
    private Integer cantidadBeneficio;

    /**
     * Qué tipo de descuento recibe el grupo BENEFICIO.
     * GRATIS     → precio = 0
     * PORCENTAJE → descuento % definido en valorBeneficio
     * MONTO_FIJO → descuento fijo definido en valorBeneficio
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "criterio_beneficio", length = 20)
    private CriterioDescuento criterioBeneficio;

    /**
     * Valor del beneficio cuando criterioBeneficio es PORCENTAJE o MONTO_FIJO.
     * Ejemplos: 50.00 (50%) | 2500.00 ($2500 de descuento)
     * NULL cuando criterioBeneficio = GRATIS.
     */
    @Column(name = "valor_beneficio", precision = 10, scale = 2)
    private BigDecimal valorBeneficio;

    // ── Relaciones ────────────────────────────────────────────────────

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
    @Fetch(FetchMode.SUBSELECT)
    @Builder.Default
    private List<PromocionFamilia> familias = new ArrayList<>();

    @OneToMany(
        mappedBy = "promocion",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Fetch(FetchMode.SUBSELECT)
    @Builder.Default
    private List<PromocionCategoria> categorias = new ArrayList<>();

    @OneToMany(
        mappedBy = "promocion",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Fetch(FetchMode.SUBSELECT)
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