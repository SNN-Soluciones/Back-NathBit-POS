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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta del reporte de IVA por tarifa en compras")
public class ReporteIvaComprasResponse {

    // ─────────────────────────────────────────────────
    //  Metadatos
    // ─────────────────────────────────────────────────

    private String empresaNombre;
    private String empresaIdentificacion;
    private String sucursalNombre;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaEmisionDesde;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaEmisionHasta;

    private String estadoInterno;
    private List<String> tiposDocumentoConsultados;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaGeneracion;

    private String generadoPor;

    // ─────────────────────────────────────────────────
    //  Contadores
    // ─────────────────────────────────────────────────

    private Integer totalDocumentos;
    private Integer cantidadFacturas;
    private Integer cantidadTiquetes;
    private Integer cantidadNotasCredito;

    // ─────────────────────────────────────────────────
    //  Totales IVA por tarifa
    // ─────────────────────────────────────────────────

    private BigDecimal totalIva0;
    private BigDecimal totalIva1;
    private BigDecimal totalIva2;
    private BigDecimal totalIva4;
    private BigDecimal totalIva8;
    private BigDecimal totalIva13;
    private BigDecimal totalOtrosImpuestos;

    // ─────────────────────────────────────────────────
    //  Totales generales
    // ─────────────────────────────────────────────────

    private BigDecimal totalGravado;
    private BigDecimal totalExento;
    private BigDecimal totalExonerado;
    private BigDecimal totalVentaNeta;
    private BigDecimal totalImpuestos;
    private BigDecimal totalDescuentos;
    private BigDecimal totalOtrosCargos;
    private BigDecimal totalGeneral;   // suma de totalComprobante de todas las líneas

    // ─────────────────────────────────────────────────
    //  Líneas de detalle
    // ─────────────────────────────────────────────────

    private List<ReporteIvaComprasLineaDTO> lineas;

    // ─────────────────────────────────────────────────
    //  Cálculo de totales — llamar desde el Service
    // ─────────────────────────────────────────────────

    public void calcularTotales() {
        if (lineas == null || lineas.isEmpty()) {
            inicializarCeros();
            return;
        }

        totalDocumentos      = 0;
        cantidadFacturas     = 0;
        cantidadTiquetes     = 0;
        cantidadNotasCredito = 0;

        totalIva0          = BigDecimal.ZERO;
        totalIva1          = BigDecimal.ZERO;
        totalIva2          = BigDecimal.ZERO;
        totalIva4          = BigDecimal.ZERO;
        totalIva8          = BigDecimal.ZERO;
        totalIva13         = BigDecimal.ZERO;
        totalOtrosImpuestos = BigDecimal.ZERO;

        totalGravado       = BigDecimal.ZERO;
        totalExento        = BigDecimal.ZERO;
        totalExonerado     = BigDecimal.ZERO;
        totalVentaNeta     = BigDecimal.ZERO;
        totalImpuestos     = BigDecimal.ZERO;
        totalDescuentos    = BigDecimal.ZERO;
        totalOtrosCargos   = BigDecimal.ZERO;
        totalGeneral       = BigDecimal.ZERO;

        for (ReporteIvaComprasLineaDTO l : lineas) {
            totalDocumentos++;

            switch (l.getTipoDocumento() != null ? l.getTipoDocumento() : "") {
                case "FACTURA_ELECTRONICA" -> cantidadFacturas++;
                case "TIQUETE_ELECTRONICO" -> cantidadTiquetes++;
                case "NOTA_CREDITO"        -> cantidadNotasCredito++;
            }

            totalIva0           = totalIva0.add(safe(l.getIva0()));
            totalIva1           = totalIva1.add(safe(l.getIva1()));
            totalIva2           = totalIva2.add(safe(l.getIva2()));
            totalIva4           = totalIva4.add(safe(l.getIva4()));
            totalIva8           = totalIva8.add(safe(l.getIva8()));
            totalIva13          = totalIva13.add(safe(l.getIva13()));
            totalOtrosImpuestos = totalOtrosImpuestos.add(safe(l.getOtrosImpuestos()));

            totalGravado        = totalGravado.add(safe(l.getTotalGravado()));
            totalExento         = totalExento.add(safe(l.getTotalExento()));
            totalExonerado      = totalExonerado.add(safe(l.getTotalExonerado()));
            totalVentaNeta      = totalVentaNeta.add(safe(l.getTotalVentaNeta()));
            totalImpuestos      = totalImpuestos.add(safe(l.getTotalImpuesto()));
            totalDescuentos     = totalDescuentos.add(safe(l.getTotalDescuentos()));
            totalOtrosCargos    = totalOtrosCargos.add(safe(l.getTotalOtrosCargos()));
            totalGeneral        = totalGeneral.add(safe(l.getTotalComprobante()));
        }
    }

    private void inicializarCeros() {
        totalDocumentos = 0; cantidadFacturas = 0; cantidadTiquetes = 0; cantidadNotasCredito = 0;
        totalIva0 = totalIva1 = totalIva2 = totalIva4 = totalIva8 = totalIva13
            = totalOtrosImpuestos = BigDecimal.ZERO;
        totalGravado = totalExento = totalExonerado = totalVentaNeta = totalImpuestos
            = totalDescuentos = totalOtrosCargos = totalGeneral = BigDecimal.ZERO;
    }

    private BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}