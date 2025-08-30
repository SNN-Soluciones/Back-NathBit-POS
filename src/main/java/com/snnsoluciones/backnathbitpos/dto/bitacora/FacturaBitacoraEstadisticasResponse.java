package com.snnsoluciones.backnathbitpos.dto.bitacora;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Estadísticas del procesamiento de facturas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaBitacoraEstadisticasResponse {
    
    // Contadores por estado
    private Long totalPendientes;
    private Long totalProcesando;
    private Long totalCompletadas;
    private Long totalConError;
    private Long totalRechazadas;
    
    // Métricas de procesamiento
    private Double porcentajeExito;
    private Double tiempoPromedioProcesamientoMinutos;
    private Integer promedioIntentos;
    
    // Por periodo (últimas 24h, 7 días, 30 días)
    private Map<String, Long> facturasPorPeriodo;
    
    // Errores comunes
    private Map<String, Long> erroresFrecuentes;
    
    // Por empresa/sucursal
    private Map<String, Long> facturasPorEmpresa;
    private Map<String, Long> facturasPorSucursal;
}