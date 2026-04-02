// src/main/java/com/snnsoluciones/backnathbitpos/entity/V2SesionPlataforma.java

package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "v2_sesion_plataforma")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class V2SesionPlataforma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_id", nullable = false)
    private V2SesionCaja sesion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plataforma_id", nullable = false)
    private PlataformaDigitalConfig plataforma;

    // Desnormalizado — el historial no cambia si se renombra la plataforma
    @Column(name = "plataforma_nombre", nullable = false, length = 100)
    private String plataformaNombre;

    @Column(name = "cantidad_pedidos", nullable = false)
    @Builder.Default
    private Integer cantidadPedidos = 0;

    @Column(name = "total_ventas", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalVentas = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}