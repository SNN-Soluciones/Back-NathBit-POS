package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "productos", 
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"codigo_interno", "empresa_id"}),
        @UniqueConstraint(columnNames = {"nombre", "empresa_id"})
    },
    indexes = {
        @Index(name = "idx_producto_empresa", columnList = "empresa_id"),
        @Index(name = "idx_producto_categoria", columnList = "categoria_id"),
        @Index(name = "idx_producto_codigo_barras", columnList = "codigo_barras")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Producto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;
    
    @Column(name = "codigo_interno", nullable = false, length = 20)
    private String codigoInterno;
    
    @Column(name = "codigo_barras", length = 30)
    private String codigoBarras;
    
    @Column(nullable = false, length = 200)
    private String nombre;
    
    @Column(columnDefinition = "TEXT")
    private String descripcion;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_cabys_id", nullable = false)
    private EmpresaCAByS empresaCabys;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private CategoriaProducto categoria;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_medida_id", nullable = false)
    private UnidadMedida unidadMedida;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moneda_id", nullable = false)
    private Moneda moneda;
    
    @Column(name = "precio_venta", nullable = false, precision = 18, scale = 5)
    private BigDecimal precioVenta;
    
    @Column(name = "aplica_servicio", nullable = false)
    @Builder.Default
    private Boolean aplicaServicio = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
    
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, 
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ProductoImpuesto> impuestos = new HashSet<>();
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Métodos helper
    public void agregarImpuesto(ProductoImpuesto impuesto) {
        impuestos.add(impuesto);
        impuesto.setProducto(this);
    }
    
    public void removerImpuesto(ProductoImpuesto impuesto) {
        impuestos.remove(impuesto);
        impuesto.setProducto(null);
    }
}