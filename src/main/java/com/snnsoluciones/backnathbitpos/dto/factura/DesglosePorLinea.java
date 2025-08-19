package com.snnsoluciones.backnathbitpos.dto.factura;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Desglose por línea para validación
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesglosePorLinea {
    
    private Integer numeroLinea;
    private String productoNombre;
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal montoTotal;
    private BigDecimal descuentos;
    private BigDecimal subtotal;
    private Boolean aplicaServicio;
    private BigDecimal montoServicio;
    private BigDecimal montoImpuesto;
    private BigDecimal total;
}