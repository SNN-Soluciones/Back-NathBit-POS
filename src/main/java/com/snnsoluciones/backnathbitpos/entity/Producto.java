package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.mh.Moneda;
import com.snnsoluciones.backnathbitpos.enums.mh.UnidadMedida;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.mapstruct.Mapper;

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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "producto_categoria",
        joinColumns = @JoinColumn(name = "producto_id"),
        inverseJoinColumns = @JoinColumn(name = "categoria_id"),
        indexes = {
            @Index(name = "idx_producto_categoria_producto", columnList = "producto_id"),
            @Index(name = "idx_producto_categoria_categoria", columnList = "categoria_id")
        }
    )
    @Builder.Default
    private Set<CategoriaProducto> categorias = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_medida", nullable = false)  // ✅ CORREGIDO
    private UnidadMedida unidadMedida = UnidadMedida.UNIDAD;

    @Enumerated(EnumType.STRING)
    @Column(name = "moneda", nullable = false)  // ✅ CORREGIDO
    private Moneda moneda = Moneda.CRC;
    
    @Column(name = "precio_venta", nullable = false, precision = 18, scale = 5)
    private BigDecimal precioVenta;
    
    @Column(name = "aplica_servicio", nullable = false)
    @Builder.Default
    private Boolean aplicaServicio = false;

    @Column(name = "es_servicio", nullable = false)
    @Builder.Default
    private Boolean esServicio = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "incluye_iva", nullable = false)
    @Builder.Default
    private Boolean incluyeIVA = true;
    
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

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(name = "imagen_key", length = 255)
    private String imagenKey;

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