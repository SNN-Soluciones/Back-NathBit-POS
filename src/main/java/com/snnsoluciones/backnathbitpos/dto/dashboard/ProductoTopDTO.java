package com.snnsoluciones.backnathbitpos.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;

/**
 * DTO para productos más vendidos (top productos)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoTopDTO {
    
    /**
     * Nombre del producto
     */
    private String nombre;
    
    /**
     * Cantidad vendida
     */
    private BigDecimal cantidad;
    
    /**
     * Monto total vendido de este producto
     */
    private BigDecimal monto;
}