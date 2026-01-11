package com.snnsoluciones.backnathbitpos.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;

/**
 * DTO con métricas por sucursal
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SucursalMetricasDTO {
    
    /**
     * ID de la sucursal
     */
    private Long id;
    
    /**
     * Nombre de la sucursal
     */
    private String nombre;
    
    /**
     * Ventas del día actual de esta sucursal
     */
    private BigDecimal ventasHoy;
    
    /**
     * Cantidad de cajas abiertas en esta sucursal
     */
    private Long cajasAbiertas;
    
    /**
     * Cantidad de PDVs activos en esta sucursal
     */
    private Long pdvsActivos;
}