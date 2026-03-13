package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.RolPromocionAlcance;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Categoría incluida en el alcance de una promoción.
 * Tabla: promocion_categorias
 *
 * El campo ROL define el papel de esta categoría dentro de la promo:
 *
 *   TRIGGER  → Productos de esta categoría ACTIVAN la promo cuando
 *              aparecen en la orden.
 *              Ejemplo: categoría "Platos Adulto" en GRUPO_CONDICIONAL.
 *
 *   BENEFICIO → Productos de esta categoría RECIBEN el descuento.
 *               Ejemplo: categoría "Menú Infantil" en GRUPO_CONDICIONAL.
 *
 * Para promos simples (PORCENTAJE, MONTO_FIJO) donde el alcance es
 * el mismo para trigger y beneficio, usar rol = TRIGGER.
 */
@Entity
@Table(
    name = "promocion_categorias",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_promocion_categoria",
        columnNames = {"promocion_id", "categoria_id", "rol"}
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