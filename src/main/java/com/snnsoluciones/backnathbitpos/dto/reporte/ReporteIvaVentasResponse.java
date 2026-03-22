package com.snnsoluciones.backnathbitpos.dto.reporte;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta completa del reporte de IVA por tarifa en ventas.
 *
 * <p>Incluye metadatos del período, los filtros aplicados, las líneas de detalle
 * y los totales consolidados por tarifa de IVA.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta del reporte de IVA por tarifa en ventas")
public class ReporteIvaVentasResponse {

    // ─────────────────────────────────────────────────
    //  Metadata
    // ─────────────────────────────────────────────────

    @Schema(description = "Nombre de la empresa", example = "Supermercado El Ahorro S.A.")
    private String empresaNombre;

    @Schema(description = "Identificación fiscal de la empresa", example = "3101234567")
    private String empresaIdentificacion;

    @Schema(description = "Nombre de la sucursal consultada", example = "Sucursal San José")
    private String sucursalNombre;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Fecha de emisión desde (filtro aplicado)", example = "2026-01-01")
    private LocalDate fechaEmisionDesde;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Fecha de emisión hasta (filtro aplicado)", example = "2026-02-28")
    private LocalDate fechaEmisionHasta;

    @Schema(description = "Estado de bitácora consultado", example = "ACEPTADA")
    private String estadoBitacora;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Fecha de aceptación/procesamiento desde (filtro aplicado)", example = "2026-02-01")
    private LocalDate fechaAceptacionDesde;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Fecha de aceptación/procesamiento hasta (filtro aplicado)", example = "2026-03-22")
    private LocalDate fechaAceptacionHasta;

    @Schema(description = "Tipos de documento incluidos en el reporte",
            example = "[\"FACTURA_ELECTRONICA\", \"TIQUETE_ELECTRONICO\", \"NOTA_CREDITO\"]")
    private List<String> tiposDocumentoConsultados;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Fecha y hora en que se generó el reporte")
    private LocalDateTime fechaGeneracion;

    @Schema(description = "Usuario que generó el reporte")
    private String generadoPor;

    // ─────────────────────────────────────────────────
    //  Contadores
    // ─────────────────────────────────────────────────

    @Schema(description = "Total de documentos encontrados", example = "152")
    private Integer totalDocumentos;

    @Schema(description = "Cantidad de facturas electrónicas", example = "120")
    private Integer cantidadFacturas;

    @Schema(description = "Cantidad de tiquetes electrónicos", example = "28")
    private Integer cantidadTiquetes;

    @Schema(description = "Cantidad de notas de crédito", example = "4")
    private Integer cantidadNotasCredito;

    // ─────────────────────────────────────────────────
    //  Totales consolidados de IVA
    // ─────────────────────────────────────────────────

    @Schema(description = "Suma total IVA 0% del período", example = "0.00")
    private BigDecimal totalIva0;

    @Schema(description = "Suma total IVA 1% del período", example = "0.00")
    private BigDecimal totalIva1;

    @Schema(description = "Suma total IVA 2% del período", example = "0.00")
    private BigDecimal totalIva2;

    @Schema(description = "Suma total IVA 4% del período", example = "5600.00")
    private BigDecimal totalIva4;

    @Schema(description = "Suma total IVA 8% del período", example = "0.00")
    private BigDecimal totalIva8;

    @Schema(description = "Suma total IVA 13% del período", example = "125000.00")
    private BigDecimal totalIva13;

    // ─────────────────────────────────────────────────
    //  Totales generales del período
    // ─────────────────────────────────────────────────

    @Schema(description = "Total venta neta del período", example = "962000.00")
    private BigDecimal totalNeto;

    @Schema(description = "Total impuestos del período (suma de todos los IVA)", example = "130600.00")
    private BigDecimal totalImpuestos;

    @Schema(description = "Total descuentos del período", example = "15000.00")
    private BigDecimal totalDescuentos;

    @Schema(description = "Total comprobantes del período (neto + impuestos)", example = "1092600.00")
    private BigDecimal totalGeneral;

    // ─────────────────────────────────────────────────
    //  Líneas de detalle
    // ─────────────────────────────────────────────────

    @Schema(description = "Lista de documentos con su desglose de IVA")
    private List<ReporteIvaVentasLineaDTO> lineas;

    // ─────────────────────────────────────────────────
    //  Cálculo de totales (se llama desde el Service)
    // ─────────────────────────────────────────────────

    /**
     * Recorre {@code lineas} y acumula todos los totales en los campos de este response.
     * También cuenta documentos por tipo.
     */
    public void calcularTotales() {

        if (lineas == null || lineas.isEmpty()) {
            inicializarCeros();
            return;
        }

        totalDocumentos   = 0;
        cantidadFacturas  = 0;
        cantidadTiquetes  = 0;
        cantidadNotasCredito = 0;

        totalIva0     = BigDecimal.ZERO;
        totalIva1     = BigDecimal.ZERO;
        totalIva2     = BigDecimal.ZERO;
        totalIva4     = BigDecimal.ZERO;
        totalIva8     = BigDecimal.ZERO;
        totalIva13    = BigDecimal.ZERO;
        totalNeto     = BigDecimal.ZERO;
        totalImpuestos = BigDecimal.ZERO;
        totalDescuentos = BigDecimal.ZERO;
        totalGeneral   = BigDecimal.ZERO;

        for (ReporteIvaVentasLineaDTO linea : lineas) {

            totalDocumentos++;

            switch (linea.getTipoDocumento() != null ? linea.getTipoDocumento() : "") {
                case "FACTURA_ELECTRONICA" -> cantidadFacturas++;
                case "TIQUETE_ELECTRONICO" -> cantidadTiquetes++;
                case "NOTA_CREDITO"        -> cantidadNotasCredito++;
            }

            totalIva0      = sumar(totalIva0,      linea.getIva0());
            totalIva1      = sumar(totalIva1,      linea.getIva1());
            totalIva2      = sumar(totalIva2,      linea.getIva2());
            totalIva4      = sumar(totalIva4,      linea.getIva4());
            totalIva8      = sumar(totalIva8,      linea.getIva8());
            totalIva13     = sumar(totalIva13,     linea.getIva13());
            totalNeto      = sumar(totalNeto,      linea.getTotalNeto());
            totalImpuestos = sumar(totalImpuestos, linea.getTotalImpuestos());
            totalDescuentos= sumar(totalDescuentos,linea.getDescuentos());
            totalGeneral   = sumar(totalGeneral,   linea.getTotal());
        }
    }

    private void inicializarCeros() {
        totalDocumentos = cantidadFacturas = cantidadTiquetes = cantidadNotasCredito = 0;
        totalIva0 = totalIva1 = totalIva2 = totalIva4 = totalIva8 = totalIva13 = BigDecimal.ZERO;
        totalNeto = totalImpuestos = totalDescuentos = totalGeneral = BigDecimal.ZERO;
    }

    private BigDecimal sumar(BigDecimal acumulado, BigDecimal valor) {
        return acumulado.add(valor != null ? valor : BigDecimal.ZERO);
    }
}