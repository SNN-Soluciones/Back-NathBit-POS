package com.snnsoluciones.backnathbitpos.dto.promociones;

import com.snnsoluciones.backnathbitpos.enums.CriterioDescuento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Producto del catálogo BENEFICIO habilitado por una promo GRUPO_CONDICIONAL.
 *
 * El frontend muestra esta lista al mesero para que elija cuál agregar
 * a la orden antes de aplicar el beneficio.
 *
 * Ejemplo "niños gratis domingos":
 *   productoId:    15
 *   nombre:        "Menú Infantil Pollo"
 *   criterio:      GRATIS
 *   precioFinal:   0.00
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoBeneficioDTO {

    private Long             productoId;
    private String           nombre;

    /** Criterio que aplica: GRATIS, PORCENTAJE o MONTO_FIJO. */
    private CriterioDescuento criterio;

    /**
     * Valor del beneficio cuando criterio = PORCENTAJE o MONTO_FIJO.
     * NULL cuando criterio = GRATIS.
     */
    private BigDecimal valorBeneficio;
}