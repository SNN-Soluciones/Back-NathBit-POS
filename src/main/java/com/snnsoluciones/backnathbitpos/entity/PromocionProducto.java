package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.RolPromocionAlcance;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Producto específico incluido en el alcance de una promoción.
 * Tabla: promocion_productos
 *
 * El campo ROL define el papel de este producto dentro de la promo:
 *
 *   TRIGGER  → Su presencia en la orden ACTIVA la promo.
 *              Ejemplo NXM simple:   las 2 hamburguesas.
 *              Ejemplo AYCE:         el ítem "Promo Alitas AYCE" ($15).
 *              Ejemplo NXM cruzado:  las 2 hamburguesas que disparan
 *                                    la soda gratis.
 *
 *   BENEFICIO → Recibe el descuento o sale gratis.
 *               Ejemplo NXM cruzado:  la soda gratis.
 *               Ejemplo AYCE:         las alitas y birras en $0.
 *               Ejemplo GRUPO_CONDICIONAL: los platos del menú infantil.
 *
 * Diferencia con PromocionItem:
 *   PromocionItem  → reglas de RONDA (cantidad_por_ronda, max_rondas)
 *                    solo para AYCE y BARRA_LIBRE.
 *   PromocionProducto → ALCANCE (¿a qué productos aplica o se beneficia?)
 *                       aplica a todos los tipos.
 */
@Entity
@Table(
    name = "promocion_productos",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_promocion_producto_alcance",
        columnNames = {"promocion_id", "producto_id", "rol"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromocionProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promocion_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Promocion promocion;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "nombre_producto", nullable = false, length = 200)
    private String nombreProducto;

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