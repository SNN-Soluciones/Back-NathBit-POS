// Archivo: src/main/java/com/snnsoluciones/backnathbitpos/entity/catalogo/CategoriaProducto.java

package com.snnsoluciones.backnathbitpos.entity.catalogo;

import com.snnsoluciones.backnathbitpos.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "categorias_producto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaProducto extends BaseEntity {

  @Column(nullable = false, unique = true, length = 50)
  private String codigo;

  @Column(nullable = false, length = 100)
  private String nombre;

  @Column(columnDefinition = "TEXT")
  private String descripcion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "categoria_padre_id")
  private CategoriaProducto categoriaPadre;

  @Column(name = "orden_visualizacion")
  @Builder.Default
  private Integer ordenVisualizacion = 0;

  @Column(name = "color_hex", length = 7)
  @Builder.Default
  private String colorHex = "#2c3e50";

  @Column(name = "icono", length = 50)
  private String icono;

  @Column(name = "imagen_url", length = 500)
  private String imagenUrl;

  @Column(name = "es_visible_menu")
  @Builder.Default
  private Boolean esVisibleMenu = true;

  @Column(name = "es_para_venta")
  @Builder.Default
  private Boolean esParaVenta = true;

  // Relaciones
  @OneToMany(mappedBy = "categoriaPadre", fetch = FetchType.LAZY)
  @Builder.Default
  private Set<CategoriaProducto> subcategorias = new HashSet<>();

  @OneToMany(mappedBy = "categoria", fetch = FetchType.LAZY)
  @Builder.Default
  private Set<Producto> productos = new HashSet<>();

  // Métodos helper
  public String getRutaCompleta() {
    if (categoriaPadre == null) {
      return nombre;
    }
    return categoriaPadre.getRutaCompleta() + " > " + nombre;
  }

  public int getNivel() {
    if (categoriaPadre == null) {
      return 0;
    }
    return categoriaPadre.getNivel() + 1;
  }
}