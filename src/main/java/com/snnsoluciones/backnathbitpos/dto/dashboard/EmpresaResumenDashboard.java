package com.snnsoluciones.backnathbitpos.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;

/**
 * DTO para resumen de empresa en dashboard administrativo
 * Incluye datos básicos y ventas del día actual
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaResumenDashboard {
    
    /**
     * ID de la empresa
     */
    private Long id;
    
    /**
     * Nombre comercial de la empresa
     */
    private String nombreComercial;
    
    /**
     * Identificación fiscal (cédula jurídica)
     */
    private String identificacion;
    
    /**
     * Total de ventas del día actual
     */
    private BigDecimal ventasHoy;
}