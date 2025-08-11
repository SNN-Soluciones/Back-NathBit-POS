package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "categorias_producto",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"nombre", "empresa_id"})
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaProducto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;
    
    @Column(nullable = false, length = 100)
    private String nombre;
    
    @Column(length = 200)
    private String descripcion;
    
    @Column(length = 7)
    private String color; // Hex color para UI
    
    @Column(length = 50)
    private String icono;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}