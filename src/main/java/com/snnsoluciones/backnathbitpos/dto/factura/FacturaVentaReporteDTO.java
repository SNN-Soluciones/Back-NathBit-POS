package com.snnsoluciones.backnathbitpos.dto.factura;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para el reporte Excel de ventas para Hacienda
 * Estructura idéntica al reporte de recepción
 * 
 * Arquitectura La Jachuda 🚀
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaVentaReporteDTO {

    // ==================== IDENTIFICACIÓN ====================

    /**
     * Tipo de documento: FE, TE, NC, ND
     */
    private String tipoDocumento;

    /**
     * Cédula/identificación del cliente
     */
    private String cedulaCliente;

    /**
     * Nombre comercial o razón social del cliente
     */
    private String nombreCliente;

    /**
     * Fecha de emisión del comprobante
     */
    private LocalDateTime fechaEmision;

    /**
     * Clave numérica de 50 dígitos
     */
    private String clave;

    /**
     * Consecutivo de la factura
     */
    private String consecutivo;

    // ==================== DESGLOSE DE IVA POR TARIFA ====================

    @Builder.Default
    private BigDecimal iva0 = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal iva1 = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal iva2 = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal iva4 = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal iva8 = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal iva13 = BigDecimal.ZERO;

    /**
     * Otros impuestos (ISC, Combustibles, Tabaco, etc.)
     */
    @Builder.Default
    private BigDecimal otrosImpuestos = BigDecimal.ZERO;

    // ==================== TOTALES SEGREGADOS - SERVICIOS ====================

    @Builder.Default
    private BigDecimal totalServiciosGravados = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalServiciosExentos = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalServiciosNoSujetos = BigDecimal.ZERO;

    // ==================== TOTALES SEGREGADOS - MERCANCÍAS ====================

    @Builder.Default
    private BigDecimal totalMercanciasGravadas = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalMercanciasExentas = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalMercanciasNoSujetas = BigDecimal.ZERO;

    // ==================== TOTALES GENERALES ====================

    private BigDecimal totalVentaNeta;

    private BigDecimal totalImpuesto;

    // ==================== OTROS TOTALES ====================

    @Builder.Default
    private BigDecimal totalDescuentos = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalOtrosCargos = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalIVADevuelto = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalExonerado = BigDecimal.ZERO;

    private BigDecimal totalComprobante;

    // ==================== CONTROL ====================

    /**
     * Signo para cálculo de totales:
     * +1 para Facturas y Tiquetes
     * -1 para Notas de Crédito
     */
    private int signo;

    // ==================== MÉTODOS CALCULADOS ====================

    /**
     * Total solo de IVA (suma de todas las tarifas)
     */
    public BigDecimal getTotalSoloIVA() {
        return iva0.add(iva1)
                  .add(iva2)
                  .add(iva4)
                  .add(iva8)
                  .add(iva13);
    }

    /**
     * Total de TODOS los impuestos (IVA + Otros)
     */
    public BigDecimal getTotalTodosImpuestos() {
        return getTotalSoloIVA().add(otrosImpuestos);
    }
}