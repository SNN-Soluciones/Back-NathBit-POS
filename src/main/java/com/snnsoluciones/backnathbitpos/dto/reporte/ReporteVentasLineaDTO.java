package com.snnsoluciones.backnathbitpos.dto.reporte;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para cada línea del reporte de ventas
 * Representa una factura/tiquete/nota con todos sus totales
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteVentasLineaDTO {
    
    // Identificación del documento
    private String clave;
    private String consecutivo;
    private String tipoDocumento;
    private LocalDateTime fechaEmision;
    
    // Datos del emisor
    private String actividadEconomicaCodigo;
    private String actividadEconomicaDescripcion;
    
    // Datos del cliente
    private String clienteNombre;
    private String clienteIdentificacion;
    private String clienteTipoIdentificacion;
    
    // Totales de mercancías
    private BigDecimal totalMercanciasGravadas;
    private BigDecimal totalMercanciasExentas;
    private BigDecimal totalMercanciasExoneradas;
    
    // Totales de servicios
    private BigDecimal totalServiciosGravados;
    private BigDecimal totalServiciosExentos; 
    private BigDecimal totalServiciosExonerados;
    
    // Subtotales agrupados
    private BigDecimal subtotalGravado;
    private BigDecimal subtotalExento;
    private BigDecimal subtotalExonerado;
    
    // Totales finales
    private BigDecimal totalVentaNeta;
    private BigDecimal totalImpuesto;
    private BigDecimal totalDescuentos;
    private BigDecimal montoTotalExonerado; // Total de impuesto exonerado
    private BigDecimal totalOtrosCargos;
    private BigDecimal totalComprobante;
    
    // Información adicional
    private String moneda;
    private BigDecimal tipoCambio;
    private String estado;
    
    // Getters calculados para el reporte
    public BigDecimal getSubtotalGravado() {
        if (subtotalGravado != null) return subtotalGravado;
        return sumarBigDecimals(totalMercanciasGravadas, totalServiciosGravados);
    }
    
    public BigDecimal getSubtotalExento() {
        if (subtotalExento != null) return subtotalExento;
        return sumarBigDecimals(totalMercanciasExentas, totalServiciosExentos);
    }
    
    public BigDecimal getSubtotalExonerado() {
        if (subtotalExonerado != null) return subtotalExonerado;
        return sumarBigDecimals(totalMercanciasExoneradas, totalServiciosExonerados);
    }
    
    // Helper para sumar BigDecimal nullables
    private BigDecimal sumarBigDecimals(BigDecimal... valores) {
        BigDecimal suma = BigDecimal.ZERO;
        for (BigDecimal valor : valores) {
            if (valor != null) {
                suma = suma.add(valor);
            }
        }
        return suma;
    }
    
    // Para notas de crédito, invertir el signo
    public void ajustarParaNotaCredito() {
        if ("NOTA_CREDITO".equals(tipoDocumento)) {
            // Invertir todos los montos
            totalMercanciasGravadas = negar(totalMercanciasGravadas);
            totalMercanciasExentas = negar(totalMercanciasExentas);
            totalMercanciasExoneradas = negar(totalMercanciasExoneradas);
            totalServiciosGravados = negar(totalServiciosGravados);
            totalServiciosExentos = negar(totalServiciosExentos);
            totalServiciosExonerados = negar(totalServiciosExonerados);
            totalVentaNeta = negar(totalVentaNeta);
            totalImpuesto = negar(totalImpuesto);
            totalDescuentos = negar(totalDescuentos);
            montoTotalExonerado = negar(montoTotalExonerado);
            totalOtrosCargos = negar(totalOtrosCargos);
            totalComprobante = negar(totalComprobante);
        }
    }
    
    private BigDecimal negar(BigDecimal valor) {
        return valor != null ? valor.negate() : null;
    }
}