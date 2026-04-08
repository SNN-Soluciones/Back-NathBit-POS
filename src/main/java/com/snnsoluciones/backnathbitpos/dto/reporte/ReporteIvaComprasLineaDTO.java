package com.snnsoluciones.backnathbitpos.dto.reporte;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Una fila del reporte de IVA por tarifa en compras.
 *
 * Cada fila representa un documento fiscal recibido de un proveedor.
 * Las NOTA_CREDITO ya vienen con signo negativo en los montos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Fila del reporte de IVA por tarifa en compras")
public class ReporteIvaComprasLineaDTO {

    // ─────────────────────────────────────────────────
    //  Identificación
    // ─────────────────────────────────────────────────

    @Schema(description = "Tipo de documento fiscal", example = "FACTURA_ELECTRONICA")
    private String tipoDocumento;

    @Schema(description = "Clave numérica de 50 dígitos", example = "50601012600310208710100100010000000011000000001")
    private String clave;

    @Schema(description = "Consecutivo del comprobante", example = "00100100010000000011")
    private String consecutivo;

    // ─────────────────────────────────────────────────
    //  Proveedor (emisor del documento)
    // ─────────────────────────────────────────────────

    @Schema(description = "Nombre o razón social del proveedor", example = "Distribuidora XYZ S.A.")
    private String proveedorNombre;

    @Schema(description = "Número de identificación del proveedor", example = "3101234567")
    private String proveedorIdentificacion;

    // ─────────────────────────────────────────────────
    //  Fecha
    // ─────────────────────────────────────────────────

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Fecha y hora de emisión del documento", example = "2026-02-15T10:30:00")
    private LocalDateTime fechaEmision;

    // ─────────────────────────────────────────────────
    //  Desglose de IVA por tarifa (código impuesto = '01')
    //  Los montos son impuesto_neto (ya restan exoneraciones)
    // ─────────────────────────────────────────────────

    @Schema(description = "IVA tarifa 0% — bienes exentos (códigos tarifa 01,05,10,11)", example = "0.00")
    private BigDecimal iva0;

    @Schema(description = "IVA tarifa 1% (código tarifa 02)", example = "0.00")
    private BigDecimal iva1;

    @Schema(description = "IVA tarifa 2% (código tarifa 03)", example = "0.00")
    private BigDecimal iva2;

    @Schema(description = "IVA tarifa 4% — servicios médicos, canasta básica (códigos 04,06)", example = "480.00")
    private BigDecimal iva4;

    @Schema(description = "IVA tarifa 8% — servicios profesionales (código tarifa 07)", example = "0.00")
    private BigDecimal iva8;

    @Schema(description = "IVA tarifa 13% — tarifa general (código tarifa 08)", example = "13000.00")
    private BigDecimal iva13;

    @Schema(description = "Otros impuestos (ISC, combustibles, tabaco, etc. — código impuesto ≠ 01)", example = "0.00")
    private BigDecimal otrosImpuestos;

    // ─────────────────────────────────────────────────
    //  Totales del documento (vienen de facturas_recepcion directamente)
    // ─────────────────────────────────────────────────

    @Schema(description = "Total gravado (base imponible con IVA)", example = "100000.00")
    private BigDecimal totalGravado;

    @Schema(description = "Total exento de IVA", example = "0.00")
    private BigDecimal totalExento;

    @Schema(description = "Total exonerado (IVA que fue eximido)", example = "0.00")
    private BigDecimal totalExonerado;

    @Schema(description = "Venta neta (después de descuentos, antes de impuestos)", example = "100000.00")
    private BigDecimal totalVentaNeta;

    @Schema(description = "Total de impuestos del comprobante (IVA + otros)", example = "13000.00")
    private BigDecimal totalImpuesto;

    @Schema(description = "Total de descuentos aplicados", example = "0.00")
    private BigDecimal totalDescuentos;

    @Schema(description = "Total de otros cargos (servicio 10%, timbre Cruz Roja, etc.)", example = "0.00")
    private BigDecimal totalOtrosCargos;

    @Schema(description = "Total final del comprobante", example = "113000.00")
    private BigDecimal totalComprobante;
}