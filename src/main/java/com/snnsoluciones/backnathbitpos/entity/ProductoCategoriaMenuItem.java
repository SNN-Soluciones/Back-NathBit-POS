// ProductoCategoriaMenuItem.java
package com.snnsoluciones.backnathbitpos.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "producto_categoria_menu_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoCategoriaMenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "categoria_menu_id", nullable = false)
    private Long categoriaMenuId;

    @Column(name = "producto_hijo_id", nullable = false)
    private Long productoHijoId;

    @Column(name = "orden", nullable = false)
    private Integer orden = 0;

    @Column(name = "destacado")
    private Boolean destacado = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relaciones (lazy loading)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_menu_id", insertable = false, updatable = false)
    private Producto categoriaMenu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_hijo_id", insertable = false, updatable = false)
    private Producto productoHijo;
}
