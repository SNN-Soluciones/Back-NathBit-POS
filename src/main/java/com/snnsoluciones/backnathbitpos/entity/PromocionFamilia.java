package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Familia incluida en el alcance de una promoción.
 * Tabla: tenant_X.promocion_familias
 *
 * Si esta tabla tiene registros para una promo, la promo aplica
 * SOLO a productos que pertenezcan a esas familias.
 *
 * familia_id se guarda como Long SIN FK explícita, siguiendo el
 * mismo patrón de Producto.familiaId en el sistema.
 * nombre_familia desnormalizado para evitar JOINs en lectura.
 */
@Entity
@Table(
    name = "promocion_familias",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_promocion_familia",
        columnNames = {"promocion_id", "familia_id"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromocionFamilia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promocion_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Promocion promocion;

    @Column(name = "familia_id", nullable = false)
    private Long familiaId;

    @Column(name = "nombre_familia", nullable = false, length = 200)
    private String nombreFamilia;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}