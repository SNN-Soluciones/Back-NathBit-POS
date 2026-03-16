package com.snnsoluciones.backnathbitpos.dto.reporte;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReporteVentasProDTO {

    // ── Cabecera ──────────────────────────────────────────────────────
    private String empresaNombre;
    private String sucursalNombre; // null si es consolidado empresa
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private boolean modoEmpresa;   // true = consolidado todas las sucursales

    // ── Resumen ejecutivo ─────────────────────────────────────────────
    private BigDecimal totalVentasFE;
    private BigDecimal totalVentasInternas;
    private BigDecimal totalVentas;
    private BigDecimal totalImpuesto;
    private BigDecimal totalDescuentos;
    private Integer cantidadDocumentosFE;
    private Integer cantidadDocumentosInternos;
    private Integer cantidadDocumentosTotal;

    // ── Comparativo período anterior ──────────────────────────────────
    private BigDecimal totalVentasPeriodoAnterior;
    private BigDecimal variacionPorcentual; // % vs periodo anterior

    // ── Ventas por día ────────────────────────────────────────────────
    private List<VentaDiaDTO> ventasPorDia;

    // ── Ventas por medio de pago ──────────────────────────────────────
    private List<MedioPagoDTO> ventasPorMedioPago;

    // ── Top productos ─────────────────────────────────────────────────
    private List<TopProductoDTO> topProductos;

    // ── Por sucursal (solo si modoEmpresa=true) ───────────────────────
    private List<SucursalResumenDTO> ventasPorSucursal;

    // ═══ DTOs internos ═══════════════════════════════════════════════

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class VentaDiaDTO {
        private LocalDate fecha;
        private String diaSemana;
        private BigDecimal ventasFE;
        private BigDecimal ventasInternas;
        private BigDecimal total;
        private BigDecimal impuesto;
        private Integer cantidadFE;
        private Integer cantidadInternas;
        // Comparativo mismo día periodo anterior
        private BigDecimal totalPeriodoAnterior;
        private BigDecimal variacion;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MedioPagoDTO {
        private String medioPago;
        private String descripcion;
        private BigDecimal monto;
        private Integer cantidad;
        private BigDecimal porcentaje;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TopProductoDTO {
        private Long productoId;
        private String nombre;
        private String categoria;
        private BigDecimal cantidadVendida;
        private BigDecimal totalVentas;
        private BigDecimal porcentaje;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SucursalResumenDTO {
        private Long sucursalId;
        private String sucursalNombre;
        private BigDecimal totalVentas;
        private BigDecimal totalFE;
        private BigDecimal totalInternas;
        private Integer cantidadDocumentos;
        private BigDecimal porcentaje;
    }
}