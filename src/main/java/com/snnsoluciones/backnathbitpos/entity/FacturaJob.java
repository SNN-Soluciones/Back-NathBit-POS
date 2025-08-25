package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoProcesoJob;
import com.snnsoluciones.backnathbitpos.enums.facturacion.PasoFacturacion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "factura_job",
    indexes = {
        @Index(name = "ix_job_estado_paso", columnList = "estado_proceso,paso_actual"),
        @Index(name = "ix_job_proxima_ejec", columnList = "proxima_ejecucion"),
        @Index(name = "ix_job_claimed_at", columnList = "claimed_at")
    })
public class FacturaJob {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación blanda para evitar ciclos pesados en JPA
    @Column(name = "factura_id", nullable = false)
    private Long facturaId;

    @Column(name = "clave", nullable = false, length = 50, unique = true)
    private String clave;

    @Enumerated(EnumType.STRING)
    @Column(name = "paso_actual", nullable = false, length = 40)
    private PasoFacturacion pasoActual;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_proceso", nullable = false, length = 40)
    private EstadoProcesoJob estadoProceso;

    @Column(name = "intentos", nullable = false)
    private int intentos;

    @Column(name = "max_intentos", nullable = false)
    private int maxIntentos;

    @Column(name = "proxima_ejecucion", nullable = false)
    private LocalDateTime proximaEjecucion;

    // Lock/coordinación
    @Column(name = "claimed_by", length = 100)
    private String claimedBy;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    // Auditoría
    @Column(name = "ultimo_error", length = 2000)
    private String ultimoError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (maxIntentos <= 0) maxIntentos = 5;
        if (proximaEjecucion == null) proximaEjecucion = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean puedeReintentarse() {
        return intentos < 5 && estadoProceso.puedeReintentar();
    }

    public void incrementarIntentos() {
        this.intentos++;
        // Backoff exponencial: 1min, 2min, 4min, 8min...
        int minutosEspera = (int) Math.pow(2, intentos - 1);
        this.proximaEjecucion = LocalDateTime.now().plusMinutes(minutosEspera);
    }
}