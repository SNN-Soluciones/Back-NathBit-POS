package com.snnsoluciones.backnathbitpos.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;

/**
 * DTO con métricas generales de la empresa
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricasDTO {
    
    /**
     * Total de ventas del día actual
     */
    private BigDecimal ventasHoy;
    
    /**
     * Total de ventas de los últimos 7 días
     */
    private BigDecimal ventasSemana;
    
    /**
     * Total de ventas del mes actual
     */
    private BigDecimal ventasMes;
    
    /**
     * Cantidad de cajas abiertas actualmente
     */
    private Long cajasAbiertas;
    
    /**
     * Cantidad de PDVs/Terminales activos
     */
    private Long pdvsActivos;
    
    /**
     * Cantidad de usuarios activos de la empresa
     */
    private Long usuariosActivos;
}