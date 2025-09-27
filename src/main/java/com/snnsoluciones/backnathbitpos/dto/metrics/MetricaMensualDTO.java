package com.snnsoluciones.backnathbitpos.dto.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricaMensualDTO {
    private Long id;
    private Long empresaId;
    private String empresaNombre;
    private Long sucursalId;
    private String sucursalNombre;
    private Integer numeroSucursal; // Usando numeroSucursal como me indicaste
    private Integer anio;
    private Integer mes;
    
    // Ventas
    private BigDecimal ventasMh;
    private BigDecimal ventasInternas;
    private BigDecimal ventasTotales;
    private BigDecimal ventasServicios;
    private BigDecimal ventasMercancias;
    
    // Deducciones
    private BigDecimal notasCreditoTotal;
    private BigDecimal anulacionesTotal;
    private BigDecimal descuentosTotal;
    
    // Impuestos
    private BigDecimal impuestoTotal;
    private BigDecimal impuestoIva13;
    private BigDecimal impuestoIva4;
    private BigDecimal impuestoIva2;
    private BigDecimal impuestoIva1;
    
    // Exenciones
    private BigDecimal exentoTotal;
    private BigDecimal exoneradoTotal;
    
    // Contadores
    private Integer cantidadFacturasMh;
    private Integer cantidadFacturasInternas;
    private Integer cantidadNotasCredito;
    private Integer cantidadAnulaciones;
    
    // Cálculos adicionales útiles
    private BigDecimal ventasNetas; // ventas - notas crédito - descuentos
    private BigDecimal baseGravable; // ventas netas - exento - exonerado
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}