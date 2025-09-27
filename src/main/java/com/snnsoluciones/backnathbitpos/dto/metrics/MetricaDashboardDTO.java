package com.snnsoluciones.backnathbitpos.dto.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricaDashboardDTO {
    // Métricas del mes actual
    private BigDecimal ventasMesActual;
    private Integer facturasMesActual;
    
    // Métricas del día
    private BigDecimal ventasHoy;
    private Integer facturasHoy;
    
    // Comparativas (opcional)
    private BigDecimal ventasMesAnterior;
    private Double porcentajeCrecimiento;
    
    // Por tipo
    private BigDecimal ventasMH;
    private BigDecimal ventasInternas;
    
    // Estado de cajas (esto vendría de otro lado)
    private Integer cajasAbiertas;
    private Integer cajasTotal;
}