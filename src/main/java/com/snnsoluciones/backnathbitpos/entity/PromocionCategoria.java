package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Categoría incluida en el alcance de una promoción.
 * Tabla: tenant_X.promocion_categorias
 *
 * Si esta tabla tiene registros para una promo, la promo aplica
 * SOLO a productos que pertenezcan a esas categorías.
 *
 * categoria_id se guarda como Long SIN FK explícita para mantener
 * consistencia con el patrón del sistema (evitar problemas cross-schema).
 * nombre_categoria desnormalizado para evitar JOINs en lectura.
 */
@Entity
@Table(
    name = "promocion_categorias",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_promocion_categoria",
        columnNames = {"promocion_id", "categoria_id"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromocionCategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promocion_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Promocion promocion;

    @Column(name = "categoria_id", nullable = false)
    private Long categoriaId;

    @Column(name = "nombre_categoria", nullable = false, length = 200)
    private String nombreCategoria;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}