package com.snnsoluciones.backnathbitpos.dto.movimiento;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de respuesta para el historial completo de movimientos
 * Incluye la lista de movimientos + totales segregados
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialMovimientosResponse {
    
    /**
     * ID de la sesión de caja
     */
    private Long sesionCajaId;
    
    /**
     * Lista completa de movimientos ordenados por fecha DESC
     */
    private List<MovimientoCajaDTO> movimientos;
    
    // ===== TOTALES SEGREGADOS =====
    
    /**
     * Total de todas las entradas
     */
    private BigDecimal totalEntradas;
    
    /**
     * Total de todas las salidas
     */
    private BigDecimal totalSalidas;
    
    /**
     * Total de vales (SALIDA_VALE)
     */
    private BigDecimal totalVales;
    
    /**
     * Total de arqueos (SALIDA_ARQUEO)
     */
    private BigDecimal totalArqueos;
    
    /**
     * Total de pagos a proveedores (SALIDA_PAGO_PROVEEDOR)
     */
    private BigDecimal totalPagosProveedores;
    
    /**
     * Total de otros gastos (SALIDA_OTROS)
     */
    private BigDecimal totalOtros;
    
    /**
     * Cantidad total de movimientos
     */
    private Integer cantidadMovimientos;
}