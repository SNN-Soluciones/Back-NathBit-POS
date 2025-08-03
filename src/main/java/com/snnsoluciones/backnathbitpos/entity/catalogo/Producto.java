// Archivo: src/main/java/com/snnsoluciones/backnathbitpos/entity/catalogo/Producto.java

package com.snnsoluciones.backnathbitpos.entity.catalogo;

import com.snnsoluciones.backnathbitpos.entity.base.BaseEntity;
import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "productos",
    indexes = {
        @Index(name = "idx_producto_codigo", columnList = "codigo"),
        @Index(name = "idx_producto_codigo_barras", columnList = "codigo_barras"),
        @Index(name = "idx_producto_nombre", columnList = "nombre")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto extends BaseEntity {

  @Column(nullable = false, unique = true, length = 50)
  private String codigo;

  @Column(name = "codigo_barras", length = 50)
  private String codigoBarras;

  @Column(nullable = false, length = 200)
  private String nombre;

  @Column(name = "nombre_corto", length = 50)
  private String nombreCorto;

  @Column(columnDefinition = "TEXT")
  private String descripcion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "categoria_id", nullable = false)
  private CategoriaProducto categoria;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private TipoProducto tipo = TipoProducto.PRODUCTO;

  // Precios
  @Column(name = "precio_venta", nullable = false, precision = 18, scale = 2)
  private BigDecimal precioVenta;

  @Column(name = "precio_costo", precision = 18, scale = 2)
  private BigDecimal precioCosto;

  @Column(name = "precio_minimo", precision = 18, scale = 2)
  private BigDecimal precioMinimo;

  // Impuestos
  @Column(name = "tarifa_iva", precision = 5, scale = 2)
  @Builder.Default
  private BigDecimal tarifaIva = new BigDecimal("13.00");

  @Column(name = "exento_iva")
  @Builder.Default
  private Boolean exentoIva = false;

  @Column(name = "incluye_iva")
  @Builder.Default
  private Boolean incluyeIva = true;

  // Control de inventario
  @Column(name = "controla_inventario")
  @Builder.Default
  private Boolean controlaInventario = true;

  @Column(name = "stock_minimo", precision = 10, scale = 3)
  @Builder.Default
  private BigDecimal stockMinimo = BigDecimal.ZERO;

  @Column(name = "stock_maximo", precision = 10, scale = 3)
  private BigDecimal stockMaximo;

  @Column(name = "unidad_medida", length = 20)
  @Builder.Default
  private String unidadMedida = "Unid";

  // Configuración para restaurante
  @Column(name = "es_menu")
  @Builder.Default
  private Boolean esMenu = false;

  @Column(name = "tiempo_preparacion")
  private Integer tiempoPreparacion; // En minutos

  @Column(name = "estacion_preparacion", length = 50)
  private String estacionPreparacion;

  @Column(name = "permite_modificadores")
  @Builder.Default
  private Boolean permiteModificadores = false;

  @Column(name = "es_modificador")
  @Builder.Default
  private Boolean esModificador = false;

  // Imágenes
  @Column(name = "imagen_url", length = 500)
  private String imagenUrl;

  @Column(name = "imagen_thumbnail_url", length = 500)
  private String imagenThumbnailUrl;

  // Información nutricional (opcional)
  @Column(name = "calorias")
  private Integer calorias;

  @Column(name = "es_vegetariano")
  @Builder.Default
  private Boolean esVegetariano = false;

  @Column(name = "es_vegano")
  @Builder.Default
  private Boolean esVegano = false;

  @Column(name = "es_sin_gluten")
  @Builder.Default
  private Boolean esSinGluten = false;

  // Relaciones
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "productos_modificadores",
      joinColumns = @JoinColumn(name = "producto_id"),
      inverseJoinColumns = @JoinColumn(name = "modificador_id")
  )
  @Builder.Default
  private Set<Producto> modificadores = new HashSet<>();

  // Métodos helper
  public BigDecimal getPrecioVentaSinIva() {
    if (incluyeIva && !exentoIva) {
      BigDecimal divisor = BigDecimal.ONE.add(tarifaIva.divide(new BigDecimal("100")));
      return precioVenta.divide(divisor, 2, BigDecimal.ROUND_HALF_UP);
    }
    return precioVenta;
  }

  public BigDecimal getMontoIva() {
    if (exentoIva) {
      return BigDecimal.ZERO;
    }
    BigDecimal base = getPrecioVentaSinIva();
    return base.multiply(tarifaIva).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
  }
}