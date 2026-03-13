package com.snnsoluciones.backnathbitpos.dto.promociones;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Representa el descuento calculado para un ítem específico de la orden.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemDescuentoDTO {

    private Long ordenItemId;
    private Long productoId;
    private String nombreProducto;
    private BigDecimal precioOriginal;
    private BigDecimal descuento;        // monto en $ a descontar
    private BigDecimal precioFinal;
    private String motivo;               // "NXM - más barato", "GRUPO_CONDICIONAL", etc.
}