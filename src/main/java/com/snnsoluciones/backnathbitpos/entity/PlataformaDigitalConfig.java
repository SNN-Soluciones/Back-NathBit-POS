package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "plataforma_digital_config",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"empresa_id", "codigo"})
    },
    indexes = {
        @Index(name = "idx_plataforma_empresa", columnList = "empresa_id"),
        @Index(name = "idx_plataforma_activo", columnList = "activo")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlataformaDigitalConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    // Código interno único por empresa (UBER, RAPPI, DIDI)
    @Column(nullable = false, length = 20)
    private String codigo;

    // Nombre amigable (UberEats, Rappi, Didi Food)
    @Column(nullable = false, length = 100)
    private String nombre;

    // Porcentaje de incremento (25.00 = 25%)
    @Column(name = "porcentaje_incremento", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal porcentajeIncremento = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    // Para UI
    @Column(name = "color_hex", length = 7)
    private String colorHex; // #FF9900

    @Column(length = 50)
    private String icono; // uber-icon.svg o fa-uber

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Usuario createdBy;

    // Métodos helper
    public BigDecimal getMultiplicador() {
        // 25% = 1.25, 20% = 1.20
        return BigDecimal.ONE.add(porcentajeIncremento.divide(new BigDecimal("100")));
    }

    public BigDecimal aplicarIncremento(BigDecimal precioBase) {
        return precioBase.multiply(getMultiplicador());
    }
}