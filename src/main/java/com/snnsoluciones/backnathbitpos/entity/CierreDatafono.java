package com.snnsoluciones.backnathbitpos.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad para registrar los cierres de datafonos por sesión de caja
 */
@Entity
@Table(name = "cierre_datafono")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CierreDatafono {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_caja_id", nullable = false)
    @JsonIgnore
    private SesionCaja sesionCaja;

    @Column(name = "datafono", nullable = false, length = 100)
    private String datafono;

    @Column(name = "monto", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal monto = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}