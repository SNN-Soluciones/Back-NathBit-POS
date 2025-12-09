package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import com.snnsoluciones.backnathbitpos.enums.TipoInventario;
import com.snnsoluciones.backnathbitpos.enums.ZonaPreparacion;
import com.snnsoluciones.backnathbitpos.enums.mh.Moneda;
import com.snnsoluciones.backnathbitpos.enums.mh.UnidadMedida;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Entity
@Table(name = "productos",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"codigo_interno", "empresa_id"}),
        @UniqueConstraint(columnNames = {"nombre", "empresa_id"})
    },
    indexes = {
        @Index(name = "idx_producto_empresa", columnList = "empresa_id"),
        @Index(name = "idx_producto_sucursal", columnList = "sucursal_id"),
        @Index(name = "idx_producto_codigo_barras", columnList = "codigo_barras"),
        @Index(name = "idx_producto_tipo", columnList = "tipo"),
        @Index(name = "idx_producto_tipo_inventario", columnList = "tipo_inventario")
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sucursal_id", nullable = true)
  private Sucursal sucursal;

  @Column(name = "codigo_interno", nullable = false, length = 20)
  private String codigoInterno;

  @Column(name = "codigo_barras", length = 30)
  private String codigoBarras;

  @Column(nullable = false, length = 200)
  private String nombre;

  @Column(columnDefinition = "TEXT")
  private String descripcion;

  // Relación con CABYS
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "empresa_cabys_id")
  private EmpresaCAByS empresaCabys;

  /**
   * Familia de productos a la que pertenece (opcional)
   * Permite agrupar productos similares
   * Ejemplo: BEBIDAS, PROTEÍNAS, EXTRAS
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "familia_id",
      nullable = true,
      foreignKey = @ForeignKey(name = "fk_producto_familia")
  )
  private FamiliaProducto familia;

  // Categorías
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

  // CAMPOS ACTUALIZADOS PARA FASE 2

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo", nullable = false, columnDefinition = "tipo_producto")
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Builder.Default
  private TipoProducto tipo = TipoProducto.VENTA;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_inventario", nullable = false, length = 20)
  @Builder.Default
  @JdbcType(PostgreSQLEnumJdbcType.class)
  private TipoInventario tipoInventario = TipoInventario.SIMPLE;

  @Enumerated(EnumType.STRING)
  @Column(name = "zona_preparacion", nullable = false, length = 20)
  @Builder.Default
  @JdbcType(PostgreSQLEnumJdbcType.class)
  private ZonaPreparacion zonaPreparacion = ZonaPreparacion.NINGUNA;

  @OneToOne(mappedBy = "producto", fetch = FetchType.LAZY)
  private ProductoCompuesto productoCompuesto;

  @Column(name = "factor_conversion_receta", precision = 10, scale = 4)
  @Builder.Default
  private BigDecimal factorConversionReceta = BigDecimal.ONE;

  @Column(name = "requiere_personalizacion", nullable = false)
  @Builder.Default
  private Boolean requierePersonalizacion = false;

  @Column(name = "precio_base", precision = 18, scale = 5)
  private BigDecimal precioBase; // Para productos compuestos

  // Campos existentes
  @Enumerated(EnumType.STRING)
  @Column(name = "unidad_medida", nullable = false)
  @Builder.Default
  private UnidadMedida unidadMedida = UnidadMedida.UNIDAD;

  @Enumerated(EnumType.STRING)
  @Column(name = "moneda", nullable = false)
  @Builder.Default
  private Moneda moneda = Moneda.CRC;

  @Column(name = "precio_venta", nullable = false, precision = 18, scale = 5)
  private BigDecimal precioVenta;

  @Column(name = "es_servicio", nullable = false)
  @Builder.Default
  private Boolean esServicio = false;

  @Column(nullable = false)
  @Builder.Default
  private Boolean activo = true;

  @Column(name = "incluye_iva", nullable = false)
  @Builder.Default
  private Boolean incluyeIVA = true;

  @Builder.Default
  private Boolean requiereInventario = false;

  @Column(name = "thumbnail_url")
  private String thumbnailUrl;

  @Column(name = "thumbnail_key")
  private String thumbnailKey;

  // Impuestos
  @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL,
      orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private Set<ProductoImpuesto> impuestos = new HashSet<>();

  // Campos de auditoría
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  // Campos de imagen
  @Column(name = "imagen_url", length = 500)
  private String imagenUrl;

  @Column(name = "imagen_key", length = 255)
  private String imagenKey;

  // Campos de compra (para materia prima y mixtos)
  @Column(name = "unidad_medida_compra", length = 50)
  private String unidadMedidaCompra;

  @Column(name = "precio_compra", precision = 10, scale = 2)
  private BigDecimal precioCompra;

  @Column(name = "factor_conversion", precision = 10, scale = 4)
  private BigDecimal factorConversion;

  @Column(name = "unidad_medida_uso", length = 50)
  private String unidadMedidaUso;

  @Column(name = "requiere_receta", nullable = false)
  @Builder.Default
  private Boolean requiereReceta = false;     // ¿Necesita receta para producirse?

  @Column(name = "ultimo_precio_compra")
  private BigDecimal ultimoPrecioCompra;

  @Column(name = "fecha_ultima_compra")
  private LocalDateTime fechaUltimaCompra;

  @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<ProductoCodigoProveedor> codigosProveedor = new ArrayList<>();


  // MÉTODOS HELPER

  /**
   * Verifica si el producto puede venderse directamente
   */
  public boolean esVendible() {
    return tipo == TipoProducto.VENTA ||
        tipo == TipoProducto.MIXTO ||
        tipo == TipoProducto.COMBO ||
        tipo == TipoProducto.COMPUESTO;
  }

  /**
   * Verifica si el producto puede usarse como ingrediente
   */
  public boolean esIngrediente() {
    return tipo == TipoProducto.MATERIA_PRIMA ||
        tipo == TipoProducto.MIXTO;
  }

  /**
   * Verifica si requiere control de inventario
   */
  public boolean requiereControlInventario() {
    return tipoInventario != TipoInventario.NINGUNO;
  }

  /**
   * Verifica si se produce con receta
   */
  public boolean seProduceConReceta() {
    return tipoInventario == TipoInventario.RECETA;
  }

  /**
   * Verifica si es un combo
   */
  public boolean esCombo() {
    return tipo == TipoProducto.COMBO;
  }

  /**
   * Verifica si es un producto compuesto personalizable
   */
  public boolean esCompuesto() {
    return tipo == TipoProducto.COMPUESTO;
  }

  /**
   * Verifica si el producto tiene familia asignada
   */
  public boolean tieneFamilia() {
    return this.familia != null;
  }

  /**
   * Obtiene el nombre de la familia o un valor por defecto
   */
  public String getNombreFamilia() {
    return tieneFamilia() ? familia.getNombre() : "Sin Familia";
  }
}