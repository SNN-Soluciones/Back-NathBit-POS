package com.snnsoluciones.backnathbitpos.dto.sesiones;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO plano para el datasource del reporte de sesión (Jasper + HTML).
 * Contiene todos los datos de un turno de cajero en una sesión SHARED.
 */
@Data
@Builder
public class TurnoReporteDTO {

    // ── Identificación ──────────────────────────────────────────
    private Long    turnoId;
    private String  usuarioNombre;
    private String  estado;          // "ACTIVA" | "CERRADA"

    // ── Tiempos ─────────────────────────────────────────────────
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;     // null si ACTIVA

    // ── Fondo al inicio ─────────────────────────────────────────
    private BigDecimal fondoInicioTurno;

    // ── Ventas por medio de pago ────────────────────────────────
    private BigDecimal ventasEfectivo;
    private BigDecimal ventasTarjeta;
    private BigDecimal ventasTransferencia;
    private BigDecimal ventasSinpe;
    private BigDecimal totalVentas;

    // ── Arqueo de efectivo ──────────────────────────────────────
    private BigDecimal montoEsperado;
    private BigDecimal montoContado;

    // ── Diferencias ─────────────────────────────────────────────
    private BigDecimal diferenciaEfectivo;
    private BigDecimal diferenciaTarjeta;
    private BigDecimal diferenciaTransferencia;
    private BigDecimal diferenciaSinpe;

    // ── Distribución ────────────────────────────────────────────
    private BigDecimal montoRetirado;
    private BigDecimal fondoCaja;

    // ── Observaciones ───────────────────────────────────────────
    private String observacionesCierre;
}