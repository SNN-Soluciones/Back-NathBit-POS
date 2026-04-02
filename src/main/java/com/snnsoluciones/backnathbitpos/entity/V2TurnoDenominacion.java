// src/main/java/com/snnsoluciones/backnathbitpos/entity/V2TurnoDenominacion.java

package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "v2_turno_denominacion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class V2TurnoDenominacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_id", nullable = false)
    private V2TurnoCajero turno;

    @Column(name = "tipo", nullable = false, length = 10)
    private String tipo; // BILLETE | MONEDA

    @Column(name = "valor", nullable = false)
    private Integer valor; // 20000, 10000, 5000...

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "subtotal", nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal; // valor * cantidad

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        // calcular subtotal automático
        if (this.valor != null && this.cantidad != null) {
            this.subtotal = BigDecimal.valueOf((long) this.valor * this.cantidad);
        }
    }
}