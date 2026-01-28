// entity/SillaBarra.java
package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.EstadoSillaBarra;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "silla_barra",
    uniqueConstraints = @UniqueConstraint(name = "uk_silla_numero_barra", columnNames = {"barra_id", "numero"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SillaBarra {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "barra_id", nullable = false, foreignKey = @ForeignKey(name = "fk_silla_barra"))
    private Barra barra;

    @Column(nullable = false)
    private Integer numero; // 1, 2, 3, 4...

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    @Builder.Default
    private EstadoSillaBarra estado = EstadoSillaBarra.DISPONIBLE;

    // ✅ Link a OrdenPersona cuando hay orden activa
    @Column(name = "orden_persona_id")
    private Long ordenPersonaId;

    @Column(name = "orden_id")
    private Long ordenId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // ✅ Helpers
    @Transient
    public boolean isDisponible() {
        return this.estado == EstadoSillaBarra.DISPONIBLE;
    }

    @Transient
    public boolean isOcupada() {
        return this.estado == EstadoSillaBarra.OCUPADA;
    }

    @Transient
    public boolean isReservada() {
        return this.estado == EstadoSillaBarra.RESERVADA;
    }

    // ✅ Marcar como ocupada con orden
    public void ocupar(Long ordenId, Long ordenPersonaId) {
        this.estado = EstadoSillaBarra.OCUPADA;
        this.ordenId = ordenId;
        this.ordenPersonaId = ordenPersonaId;
    }

    // ✅ Liberar silla
    public void liberar() {
        this.estado = EstadoSillaBarra.DISPONIBLE;
        this.ordenId = null;
        this.ordenPersonaId = null;
    }

    // ✅ Reservar silla
    public void reservar() {
        if (!isDisponible()) {
            throw new IllegalStateException("Solo se puede reservar una silla disponible");
        }
        this.estado = EstadoSillaBarra.RESERVADA;
    }
}