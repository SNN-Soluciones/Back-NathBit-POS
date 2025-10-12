package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "familia_producto",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_familia_codigo_empresa", 
                         columnNames = {"codigo", "empresa_id"})
    },
    indexes = {
        @Index(name = "idx_familia_empresa", columnList = "empresa_id"),
        @Index(name = "idx_familia_activa", columnList = "activa"),
        @Index(name = "idx_familia_orden", columnList = "orden")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamiliaProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false,
               foreignKey = @ForeignKey(name = "fk_familia_empresa"))
    private Empresa empresa;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(length = 20)
    private String color;

    @Column(length = 50)
    private String icono;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}