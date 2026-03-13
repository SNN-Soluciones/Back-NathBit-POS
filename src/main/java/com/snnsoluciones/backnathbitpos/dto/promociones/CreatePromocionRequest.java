package com.snnsoluciones.backnathbitpos.dto.promociones;

import com.snnsoluciones.backnathbitpos.enums.CriterioDescuento;
import com.snnsoluciones.backnathbitpos.enums.CriterioItemGratis;
import com.snnsoluciones.backnathbitpos.enums.TipoPromocion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Request para crear o actualizar una promoción.
 *
 * Campos requeridos según tipo:
 *   NXM              → llevaN, pagaM, criterioItemGratis
 *   BARRA_LIBRE      → precioPromo + items
 *   ALL_YOU_CAN_EAT  → precioPromo + items (con reglas de ronda)
 *                      + al menos un producto con rol TRIGGER
 *   PORCENTAJE       → porcentajeDescuento
 *   MONTO_FIJO       → montoDescuento
 *   GRUPO_CONDICIONAL → cantidadTrigger, cantidadBeneficio,
 *                       criterioBeneficio, valorBeneficio (si aplica)
 *                       + alcance TRIGGER y BENEFICIO definidos
 *   HAPPY_HOUR       → cualquier combinación + horaInicio + horaFin
 *   ESPECIAL         → descripcion + items opcionales
 *
 * La validación cruzada por tipo se hace en la capa de servicio.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePromocionRequest {

    // ── Identificación ────────────────────────────────────────────────

    @NotBlank(message = "El nombre de la promoción es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @Size(max = 300, message = "La descripción no puede superar los 300 caracteres")
    private String descripcion;

    @NotNull(message = "El tipo de promoción es obligatorio")
    private TipoPromocion tipo;

    @Builder.Default
    private Boolean activo = true;

    // ── Vigencia por fecha (opcional) ─────────────────────────────────
    // NULL en ambos = siempre activa dentro del rango de días/hora.
    // Si vienen, la promo solo aplica dentro de ese rango de fechas.

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    // ── Stacking ──────────────────────────────────────────────────────

    @Builder.Default
    private Boolean permitirStack = false;

    // ── Días activos (al menos uno requerido, validado en servicio) ───

    @Builder.Default private Boolean lunes     = false;
    @Builder.Default private Boolean martes    = false;
    @Builder.Default private Boolean miercoles = false;
    @Builder.Default private Boolean jueves    = false;
    @Builder.Default private Boolean viernes   = false;
    @Builder.Default private Boolean sabado    = false;
    @Builder.Default private Boolean domingo   = false;

    // ── Rango horario (ambos NULL = todo el día comercial) ────────────

    private LocalTime horaInicio;
    private LocalTime horaFin;

    // ── Campos para NXM ───────────────────────────────────────────────

    @Min(value = 2, message = "lleva_n debe ser al menos 2")
    private Integer llevaN;

    @Min(value = 1, message = "paga_m debe ser al menos 1")
    private Integer pagaM;

    /**
     * Cómo se elige el ítem gratis en NXM.
     * MAS_BARATO          → el de menor precio entre los BENEFICIO en la orden.
     * PRODUCTO_ESPECIFICO → definido en productos con rol BENEFICIO.
     */
    private CriterioItemGratis criterioItemGratis;

    // ── Campos para PORCENTAJE ────────────────────────────────────────

    @DecimalMin(value = "0.01", message = "El porcentaje debe ser mayor a 0")
    @DecimalMax(value = "100.00", message = "El porcentaje no puede superar 100")
    private BigDecimal porcentajeDescuento;

    // ── Campos para MONTO_FIJO ────────────────────────────────────────

    @DecimalMin(value = "0.01", message = "El monto de descuento debe ser mayor a 0")
    private BigDecimal montoDescuento;

    // ── Campos para BARRA_LIBRE y ALL_YOU_CAN_EAT ────────────────────

    @DecimalMin(value = "0.00", message = "El precio promo no puede ser negativo")
    private BigDecimal precioPromo;

    // ── Campos para GRUPO_CONDICIONAL ─────────────────────────────────

    /**
     * Cuántos ítems del grupo TRIGGER se necesitan para activar la promo.
     * Ejemplo: 2 (dos adultos deben tener items del alcance TRIGGER).
     */
    @Min(value = 1, message = "cantidad_trigger debe ser al menos 1")
    private Integer cantidadTrigger;

    /**
     * Cuántos ítems del grupo BENEFICIO reciben el descuento.
     * Ejemplo: 1 (un niño se beneficia).
     */
    @Min(value = 1, message = "cantidad_beneficio debe ser al menos 1")
    private Integer cantidadBeneficio;

    /**
     * Qué tipo de descuento recibe el grupo BENEFICIO.
     * GRATIS     → precio = 0
     * PORCENTAJE → % definido en valorBeneficio
     * MONTO_FIJO → monto fijo definido en valorBeneficio
     */
    private CriterioDescuento criterioBeneficio;

    /**
     * Valor del beneficio cuando criterioBeneficio es PORCENTAJE o MONTO_FIJO.
     * NULL cuando criterioBeneficio = GRATIS.
     */
    @DecimalMin(value = "0.01", message = "El valor del beneficio debe ser mayor a 0")
    private BigDecimal valorBeneficio;

    // ── Items (BARRA_LIBRE, ALL_YOU_CAN_EAT, ESPECIAL) ───────────────

    @Valid
    @Builder.Default
    private List<CreatePromocionItemRequest> items = new ArrayList<>();

    // ── Alcance ───────────────────────────────────────────────────────
    // Cada lista usa CreatePromocionAlcanceRequest que ya incluye rol.
    // TRIGGER  → activa la promo
    // BENEFICIO → recibe el descuento

    @Valid
    @Builder.Default
    private List<CreatePromocionAlcanceRequest> familias = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<CreatePromocionAlcanceRequest> categorias = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<CreatePromocionAlcanceRequest> productos = new ArrayList<>();
}