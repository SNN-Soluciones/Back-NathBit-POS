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
 * DTO que representa una fila del reporte de IVA por tarifa en ventas.
 *
 * <p>Cada fila corresponde a un documento fiscal (factura, tiquete o nota de crédito)
 * con el desglose de IVA por cada tarifa vigente en Costa Rica y los totales del comprobante.</p>
 *
 * <p>Para las NOTA_CREDITO todos los montos ya vienen con signo negativo,
 * por lo que la suma algebraica da el neto correcto.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Fila del reporte de IVA por tarifa")
public class ReporteIvaVentasLineaDTO {

    // ─────────────────────────────────────────────────
    //  Identificación del documento
    // ─────────────────────────────────────────────────

    @Schema(description = "Tipo de documento fiscal", example = "FACTURA_ELECTRONICA")
    private String tipoDocumento;

    @Schema(description = "Clave numérica de 50 dígitos del comprobante", example = "50609032600310208710100100010000000011000000001")
    private String clave;

    @Schema(description = "Consecutivo del comprobante", example = "00100100010000000011")
    private String consecutivo;

    // ─────────────────────────────────────────────────
    //  Datos del cliente
    // ─────────────────────────────────────────────────

    @Schema(description = "Nombre o razón social del cliente", example = "Juan Pérez Mora")
    private String clienteNombre;

    @Schema(description = "Número de identificación del cliente", example = "112340567")
    private String clienteIdentificacion;

    // ─────────────────────────────────────────────────
    //  Fechas
    // ─────────────────────────────────────────────────

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Fecha y hora de emisión del documento", example = "2026-02-15T10:30:00")
    private LocalDateTime fechaEmision;

    // ─────────────────────────────────────────────────
    //  Desglose de IVA por tarifa
    //  (negativo cuando tipoDocumento = NOTA_CREDITO)
    // ─────────────────────────────────────────────────

    @Schema(description = "Monto IVA tarifa 0% (bienes exentos)", example = "0.00")
    private BigDecimal iva0;

    @Schema(description = "Monto IVA tarifa 1%", example = "0.00")
    private BigDecimal iva1;

    @Schema(description = "Monto IVA tarifa 2%", example = "0.00")
    private BigDecimal iva2;

    @Schema(description = "Monto IVA tarifa 4% (servicios médicos, etc.)", example = "120.00")
    private BigDecimal iva4;

    @Schema(description = "Monto IVA tarifa 8% (servicios profesionales, etc.)", example = "0.00")
    private BigDecimal iva8;

    @Schema(description = "Monto IVA tarifa 13% (tarifa general)", example = "1300.00")
    private BigDecimal iva13;

    // ─────────────────────────────────────────────────
    //  Totales del comprobante
    //  (negativo cuando tipoDocumento = NOTA_CREDITO)
    // ─────────────────────────────────────────────────

    @Schema(description = "Total venta neta (sin impuestos)", example = "10000.00")
    private BigDecimal totalNeto;

    @Schema(description = "Total de impuestos del comprobante", example = "1300.00")
    private BigDecimal totalImpuestos;

    @Schema(description = "Total de descuentos aplicados", example = "500.00")
    private BigDecimal descuentos;

    @Schema(description = "Total del comprobante (neto + impuestos)", example = "11300.00")
    private BigDecimal total;
}