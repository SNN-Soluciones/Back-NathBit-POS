// src/main/java/com/snnsoluciones/backnathbitpos/entity/V2MovimientoCaja.java

package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "v2_movimiento_caja")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class V2MovimientoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ligado a la sesión Y al turno
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_id", nullable = false)
    private V2SesionCaja sesion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_id", nullable = false)
    private V2TurnoCajero turno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo;
    /*
     * Valores válidos:
     * ENTRADA_EFECTIVO         — entrada manual de efectivo
     * ENTRADA_ABONO_CREDITO    — cliente abona una deuda (no es venta)
     * SALIDA_VALE              — vale / anticipo a empleado
     * SALIDA_ARQUEO            — retiro en arqueo de turno
     * SALIDA_PAGO_PROVEEDOR    — pago a proveedor
     * SALIDA_DEPOSITO          — depósito bancario
     * SALIDA_OTROS             — otros egresos
     */

    @Column(name = "monto", nullable = false, precision = 18, scale = 2)
    private BigDecimal monto;

    @Column(name = "concepto", length = 300)
    private String concepto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autorizado_por_id")
    private Usuario autorizadoPor;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        this.createdAt = ahora;
        this.fechaHora = ahora;
    }

    // ── Helpers ──────────────────────────────────────────────
    public boolean esEntrada() {
        return this.tipo != null && this.tipo.startsWith("ENTRADA_");
    }

    public boolean esSalida() {
        return this.tipo != null && this.tipo.startsWith("SALIDA_");
    }

    public boolean esAbonoCredito() {
        return "ENTRADA_ABONO_CREDITO".equals(this.tipo);
    }
}