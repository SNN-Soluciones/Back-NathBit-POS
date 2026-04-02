// src/main/java/com/snnsoluciones/backnathbitpos/entity/V2TurnoCajero.java

package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "v2_turno_cajero")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class V2TurnoCajero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_id", nullable = false)
    private V2SesionCaja sesion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * Para el primero que abre: = sesion.montoInicial
     * Para los siguientes: calculado desde facturas reales al momento de unirse
     * sesion.montoInicial + Σef_facturas_sesion + Σentradas - Σsalidas
     */
    @Column(name = "fondo_inicio", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal fondoInicio = BigDecimal.ZERO;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "ACTIVO"; // ACTIVO | CERRADO

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    // Ventas — se llenan al cerrar el turno desde facturas reales
    @Column(name = "ventas_efectivo", precision = 18, scale = 2)
    private BigDecimal ventasEfectivo;

    @Column(name = "ventas_tarjeta", precision = 18, scale = 2)
    private BigDecimal ventasTarjeta;

    @Column(name = "ventas_sinpe", precision = 18, scale = 2)
    private BigDecimal ventasSinpe;

    @Column(name = "ventas_transferencia", precision = 18, scale = 2)
    private BigDecimal ventasTransferencia;

    @Column(name = "ventas_otros", precision = 18, scale = 2)
    private BigDecimal ventasOtros;

    @Column(name = "ventas_credito", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal ventasCredito = BigDecimal.ZERO;

    // Arqueo
    @Column(name = "monto_esperado", precision = 18, scale = 2)
    private BigDecimal montoEsperado;

    @Column(name = "monto_contado", precision = 18, scale = 2)
    private BigDecimal montoContado;

    @Column(name = "monto_retirado", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal montoRetirado = BigDecimal.ZERO;

    @Column(name = "fondo_caja", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal fondoCaja = BigDecimal.ZERO;

    // Diferencias
    @Column(name = "dif_efectivo", precision = 18, scale = 2)
    private BigDecimal difEfectivo;

    @Column(name = "dif_tarjeta", precision = 18, scale = 2)
    private BigDecimal difTarjeta;

    @Column(name = "dif_sinpe", precision = 18, scale = 2)
    private BigDecimal difSinpe;

    @Column(name = "dif_transferencia", precision = 18, scale = 2)
    private BigDecimal difTransferencia;

    @Column(name = "observaciones_cierre", length = 500)
    private String observacionesCierre;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        this.createdAt  = ahora;
        this.updatedAt  = ahora;
        this.fechaInicio = ahora;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Helpers ──────────────────────────────────────────────
    public boolean isActivo() {
        return "ACTIVO".equals(this.estado);
    }

    public boolean isCerrado() {
        return "CERRADO".equals(this.estado);
    }

    /**
     * Calcula el monto esperado en caja para este cajero.
     * Solo válido en gaveta COMPARTIDA — incluye el fondo al entrar.
     * En gaveta INDIVIDUAL es solo fondoInicio + sus ventas.
     */
    public BigDecimal calcularEsperado(BigDecimal entradasAdicionales, BigDecimal salidas) {
        BigDecimal base     = nvl(fondoInicio);
        BigDecimal ventas   = nvl(ventasEfectivo);
        BigDecimal entradas = nvl(entradasAdicionales);
        BigDecimal sal      = nvl(salidas);
        return base.add(ventas).add(entradas).subtract(sal);
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}