package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.RolPromocionAlcance;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Familia incluida en el alcance de una promoción.
 * Tabla: promocion_familias
 *
 * El campo ROL define el papel de esta familia dentro de la promo:
 *
 *   TRIGGER  → Productos de esta familia ACTIVAN la promo cuando
 *              aparecen en la orden.
 *
 *   BENEFICIO → Productos de esta familia RECIBEN el descuento.
 *
 * Para promos simples (PORCENTAJE, MONTO_FIJO) usar rol = TRIGGER.
 */
@Entity
@Table(
    name = "promocion_familias",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_promocion_familia",
        columnNames = {"promocion_id", "familia_id", "rol"}
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

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    @Builder.Default
    private RolPromocionAlcance rol = RolPromocionAlcance.TRIGGER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}