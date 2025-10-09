package com.snnsoluciones.backnathbitpos.dto.facturarecepcion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para el reporte Excel de facturas de recepción aceptadas
 * Contiene todos los totales segregados según normativa Hacienda v4.4
 *
 * Arquitectura La Jachuda 🚀
 * "Roads? Where we're going, we don't need roads." - Doc Brown
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaRecepcionReporteDTO {

    // ==================== IDENTIFICACIÓN ====================

    /**
     * Tipo de documento: FE, TE, NC, ND
     */
    private String tipoDocumento;

    /**
     * Cédula/identificación del emisor (proveedor)
     */
    private String cedulaEmisor;

    /**
     * Nombre comercial o razón social del emisor
     */
    private String nombreEmisor;

    /**
     * Fecha de emisión del comprobante
     */
    private LocalDateTime fechaEmision;

    /**
     * Clave numérica de 50 dígitos
     */
    private String clave;

    /**
     * Motivo/detalle del mensaje receptor enviado a Hacienda
     * Ejemplo: "COMPRAS", "CREDITO FISCAL", etc.
     */
    private String motivoRespuesta;

    // ==================== TOTALES SEGREGADOS - SERVICIOS ====================

    /**
     * Total de servicios gravados con IVA
     */
    @Builder.Default
    private BigDecimal totalServiciosGravados = BigDecimal.ZERO;

    /**
     * Total de servicios exentos de IVA
     */
    @Builder.Default
    private BigDecimal totalServiciosExentos = BigDecimal.ZERO;

    /**
     * Total de servicios no sujetos a IVA
     */
    @Builder.Default
    private BigDecimal totalServiciosNoSujetos = BigDecimal.ZERO;

    // ==================== TOTALES SEGREGADOS - MERCANCÍAS ====================

    /**
     * Total de mercancías gravadas con IVA
     */
    @Builder.Default
    private BigDecimal totalMercanciasGravadas = BigDecimal.ZERO;

    /**
     * Total de mercancías exentas de IVA
     */
    @Builder.Default
    private BigDecimal totalMercanciasExentas = BigDecimal.ZERO;

    /**
     * Total de mercancías no sujetas a IVA
     */
    @Builder.Default
    private BigDecimal totalMercanciasNoSujetas = BigDecimal.ZERO;

    // ==================== TOTALES GENERALES ====================

    /**
     * Total venta neta (después de descuentos)
     */
    private BigDecimal totalVentaNeta;

    /**
     * Total de impuestos (principalmente IVA)
     */
    private BigDecimal totalImpuesto;

    // ==================== DESGLOSE DE IVA POR TARIFA ====================

    /**
     * IVA con tarifa 0% (códigos: 01, 05, 10, 11)
     */
    @Builder.Default
    private BigDecimal iva0 = BigDecimal.ZERO;

    /**
     * IVA con tarifa 1% (código: 02)
     */
    @Builder.Default
    private BigDecimal iva1 = BigDecimal.ZERO;

    /**
     * IVA con tarifa 2% (código: 03)
     */
    @Builder.Default
    private BigDecimal iva2 = BigDecimal.ZERO;

    /**
     * IVA con tarifa 4% (códigos: 04, 06)
     */
    @Builder.Default
    private BigDecimal iva4 = BigDecimal.ZERO;

    /**
     * IVA con tarifa 8% (código: 07)
     */
    @Builder.Default
    private BigDecimal iva8 = BigDecimal.ZERO;

    /**
     * IVA con tarifa 13% (código: 08) - Tarifa general
     */
    @Builder.Default
    private BigDecimal iva13 = BigDecimal.ZERO;

    // ==================== OTROS TOTALES ====================

    /**
     * Total de descuentos aplicados
     */
    @Builder.Default
    private BigDecimal totalDescuentos = BigDecimal.ZERO;

    /**
     * Total de otros cargos
     */
    @Builder.Default
    private BigDecimal totalOtrosCargos = BigDecimal.ZERO;

    /**
     * Total de IVA devuelto (servicios médicos en tarjeta)
     */
    @Builder.Default
    private BigDecimal totalIVADevuelto = BigDecimal.ZERO;

    /**
     * Total exonerado
     */
    @Builder.Default
    private BigDecimal totalExonerado = BigDecimal.ZERO;

    /**
     * Total final del comprobante
     */
    private BigDecimal totalComprobante;

    // ==================== CONTROL ====================

    /**
     * Signo para cálculo de totales:
     * +1 para Facturas y Tiquetes (se suman)
     * -1 para Notas de Crédito (se restan)
     */
    private int signo;
}