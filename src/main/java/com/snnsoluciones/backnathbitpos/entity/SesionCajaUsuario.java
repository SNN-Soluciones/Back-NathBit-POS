package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sesion_caja_usuario")
public class SesionCajaUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_caja_id", nullable = false)
    private SesionCaja sesionCaja;

    @Column(name = "monto_retirado", precision = 18, scale = 2)
    private BigDecimal montoRetirado;

    @Column(name = "fondo_caja", precision = 18, scale = 2)
    private BigDecimal fondoCaja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin")
    private LocalDateTime fechaHoraFin;

    @Column(name = "diferencia_efectivo", precision = 18, scale = 2)
    private BigDecimal diferenciaEfectivo;

    @Column(name = "diferencia_tarjeta", precision = 18, scale = 2)
    private BigDecimal diferenciaTarjeta;

    @Column(name = "diferencia_sinpe", precision = 18, scale = 2)
    private BigDecimal diferenciaSinpe;

    @Column(name = "diferencia_transferencia", precision = 18, scale = 2)
    private BigDecimal diferenciaTransferencia;

    /**
     * Efectivo en caja al momento en que este cajero inició su turno.
     *
     * Cajero que abre la sesión: = montoInicial de SesionCaja.
     * Cajeros siguientes:        = montoInicial + Σ ventas efectivo todos - Σ retiros globales
     *                              (calculado con calcularMontoEsperadoEfectivoHasta() al momento de unirse)
     *
     * Este campo es la base para el reporte de arqueo individual.
     */
    @Column(name = "fondo_inicio_turno", precision = 18, scale = 2, nullable = false)
    private BigDecimal fondoInicioTurno = BigDecimal.ZERO;

    // Ventas acumuladas por este cajero en este turno
    @Column(name = "ventas_efectivo", precision = 18, scale = 2)
    private BigDecimal ventasEfectivo = BigDecimal.ZERO;

    @Column(name = "ventas_tarjeta", precision = 18, scale = 2)
    private BigDecimal ventasTarjeta = BigDecimal.ZERO;

    @Column(name = "ventas_transferencia", precision = 18, scale = 2)
    private BigDecimal ventasTransferencia = BigDecimal.ZERO;

    @Column(name = "ventas_otros", precision = 18, scale = 2)
    private BigDecimal ventasOtros = BigDecimal.ZERO;

    // Retiros de efectivo realizados durante este turno
    @Column(name = "total_retiros", precision = 18, scale = 2)
    private BigDecimal totalRetiros = BigDecimal.ZERO;

    // Devoluciones en efectivo en este turno
    @Column(name = "total_devoluciones_efectivo", precision = 18, scale = 2)
    private BigDecimal totalDevolucionesEfectivo = BigDecimal.ZERO;

    // ── Arqueo / conteo de turno ──────────────────────────────────────────────

    /**
     * Monto esperado en caja al momento del arqueo de este cajero.
     * = fondoInicioTurno + ventasEfectivo - totalRetiros
     *
     * Este campo se calcula y persiste cuando el cajero inicia el conteo,
     * para que el "esperado" no cambie mientras hace el recuento físico.
     */
    @Column(name = "monto_esperado", precision = 18, scale = 2)
    private BigDecimal montoEsperado = BigDecimal.ZERO;

    /** Lo que el cajero dice tener físicamente al contar. */
    @Column(name = "monto_contado", precision = 18, scale = 2)
    private BigDecimal montoContado;

    /** montoContado - montoEsperado. Negativo = faltante, positivo = sobrante. */
    @Column(name = "diferencia", precision = 18, scale = 2)
    private BigDecimal diferencia;

    /** Timestamp en que se inició el conteo (puede pasar tiempo hasta confirmar). */
    @Column(name = "fecha_hora_inicio_conteo")
    private LocalDateTime fechaHoraInicioConteo;

    @Column(name = "observaciones_cierre", length = 500)
    private String observacionesCierre;

    /**
     * ACTIVA  → cajero trabajando (puede facturar)
     * CONTEO  → inició el arqueo (ya no puede facturar, conteo en progreso)
     * CERRADA → arqueo confirmado, turno finalizado
     */
    @Column(name = "estado", length = 20)
    private String estado = "ACTIVA";

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Calcula el esperado de este turno al momento del arqueo.
     * Llamar antes de persistir montoEsperado.
     */
    public BigDecimal calcularEsperadoTurno() {
        BigDecimal fondo      = fondoInicioTurno  != null ? fondoInicioTurno  : BigDecimal.ZERO;
        BigDecimal ventas     = ventasEfectivo    != null ? ventasEfectivo    : BigDecimal.ZERO;
        BigDecimal retiros    = totalRetiros      != null ? totalRetiros      : BigDecimal.ZERO;
        BigDecimal devs       = totalDevolucionesEfectivo != null ? totalDevolucionesEfectivo : BigDecimal.ZERO;
        return fondo.add(ventas).subtract(retiros).subtract(devs);
    }

    public boolean estaActivo() {
        return "ACTIVA".equals(estado);
    }

    public boolean estaEnConteo() {
        return "CONTEO".equals(estado);
    }

    public boolean estaCerrado() {
        return "CERRADA".equals(estado);
    }
}