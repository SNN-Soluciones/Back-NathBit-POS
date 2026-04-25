// src/main/java/com/snnsoluciones/backnathbitpos/entity/V2SesionCaja.java

package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import java.util.List;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "v2_sesion_caja")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class V2SesionCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id", nullable = false)
    private Terminal terminal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_apertura_id", nullable = true)
    private Usuario usuarioApertura;

    @Column(name = "modo_gaveta", nullable = false, length = 20)
    @Builder.Default
    private String modoGaveta = "COMPARTIDA"; // COMPARTIDA | INDIVIDUAL

    @Column(name = "monto_inicial", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal montoInicial = BigDecimal.ZERO;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "ABIERTA"; // ABIERTA | CERRADA

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "fondo_caja")
    private BigDecimal fondoCaja;

    // Totales consolidados — se llenan al cerrar
    @Column(name = "total_efectivo", precision = 18, scale = 2)
    private BigDecimal totalEfectivo;

    @Column(name = "total_tarjeta", precision = 18, scale = 2)
    private BigDecimal totalTarjeta;

    @Column(name = "total_sinpe", precision = 18, scale = 2)
    private BigDecimal totalSinpe;

    @Column(name = "total_transferencia", precision = 18, scale = 2)
    private BigDecimal totalTransferencia;

    @Column(name = "total_otros", precision = 18, scale = 2)
    private BigDecimal totalOtros;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @OneToMany(mappedBy = "sesion", fetch = FetchType.LAZY)
    private List<V2TurnoCajero> turnos;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        this.createdAt    = ahora;
        this.updatedAt    = ahora;
        this.fechaApertura = ahora;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Helpers ──────────────────────────────────────────────
    public boolean isAbierta() {
        return "ABIERTA".equals(this.estado);
    }

    public boolean isCompartida() {
        return "COMPARTIDA".equals(this.modoGaveta);
    }
}