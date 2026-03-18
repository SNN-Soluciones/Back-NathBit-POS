package com.snnsoluciones.backnathbitpos.dto.promociones;

import com.snnsoluciones.backnathbitpos.enums.TipoPromocion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Una promoción que califica para la orden actual.
 *
 * Campos por tipo:
 *
 *   NXM / PORCENTAJE / MONTO_FIJO / HAPPY_HOUR
 *     → itemsAfectados: ítems ya en la orden que recibirán el descuento.
 *     → totalDescuento: suma total a descontar.
 *
 *   GRUPO_CONDICIONAL (ej: niños gratis domingos)
 *     → itemsAfectados: ítems ya en la orden que son BENEFICIO
 *                       y serán descontados al aplicar.
 *     → productosBeneficioDisponibles: catálogo de productos BENEFICIO
 *                       habilitados por la promo. El mesero elige de aquí
 *                       cuál agregar a la orden antes de aplicar.
 *     → cantidadBeneficioDisponible: cuántos productos BENEFICIO puede
 *                       agregar todavía el mesero (activaciones * cantidadBeneficio
 *                       menos los que ya están en orden).
 *
 *   ALL_YOU_CAN_EAT / BARRA_LIBRE
 *     → itemsInicialesAYCE: ítems que se crearán automáticamente a precio $0
 *                           al activar la promo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromocionAplicableDTO {

    private Long         promocionId;
    private String       nombre;
    private TipoPromocion tipo;

    // Ítems ya en la orden que recibirán descuento
    private List<ItemDescuentoDTO> itemsAfectados;
    private BigDecimal             totalDescuento;

    // ── GRUPO_CONDICIONAL ─────────────────────────────────────────────
    // El mesero ve esta lista y elige qué producto gratis/con descuento
    // quiere agregar para el cliente beneficiado.

    /**
     * Productos del catálogo BENEFICIO que la promo habilita.
     * Viene de PromocionProducto con rol=BENEFICIO (nombre/id desnormalizados).
     * NULL para tipos que no son GRUPO_CONDICIONAL.
     */
    private List<ProductoBeneficioDTO> productosBeneficioDisponibles;

    /**
     * Cuántos productos BENEFICIO puede agregar aún el mesero.
     * Ejemplo: 4 adultos → 2 activaciones → 2 niños habilitados,
     *          pero ya hay 1 plato niño en la orden → cantidadDisponible = 1.
     */
    private Integer cantidadBeneficioDisponible;

    // ── ALL_YOU_CAN_EAT / BARRA_LIBRE ────────────────────────────────

    /**
     * Ítems que se crearán automáticamente a precio $0 al activar el AYCE.
     * NULL para tipos que no son AYCE/BARRA_LIBRE.
     */
    private List<ItemRondaDTO> itemsInicialesAYCE;
}