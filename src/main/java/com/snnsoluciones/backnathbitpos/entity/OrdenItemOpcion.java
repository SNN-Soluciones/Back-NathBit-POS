package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "orden_item_opciones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenItemOpcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "orden_item_id", nullable = false)
    private OrdenItem ordenItemPadre;

    @ManyToOne(optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private ProductoCompuestoSlot slot;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_opcion_id", nullable = false)
    private Producto productoOpcion;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal cantidad = BigDecimal.ONE;

    // Precio adicional si aplica
    @Column(name = "precio_adicional", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal precioAdicional = BigDecimal.ZERO;

    @Column(name = "es_gratuita")
    @Builder.Default
    private Boolean esGratuita = false;

    // Para tracking
    @Column(name = "nombre_slot")
    private String nombreSlot; // Snapshot del nombre al momento de la orden

    @Column(name = "nombre_opcion")
    private String nombreOpcion; // Snapshot del nombre del producto
}