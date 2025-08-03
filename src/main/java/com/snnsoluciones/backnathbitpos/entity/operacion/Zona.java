// Archivo: src/main/java/com/snnsoluciones/backnathbitpos/entity/operacion/Zona.java

package com.snnsoluciones.backnathbitpos.entity.operacion;

import com.snnsoluciones.backnathbitpos.entity.base.BaseEntity;
import com.snnsoluciones.backnathbitpos.entity.tenant.Sucursal;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "zonas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Zona extends BaseEntity {

  @Column(nullable = false, length = 20)
  private String codigo;

  @Column(nullable = false, length = 100)
  private String nombre;

  @Column(length = 200)
  private String descripcion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sucursal_id", nullable = false)
  private Sucursal sucursal;

  // Configuración visual (para UI)
  @Column(name = "color_hex", length = 7)
  @Builder.Default
  private String colorHex = "#3498db";

  @Column(name = "orden_visualizacion")
  @Builder.Default
  private Integer ordenVisualizacion = 0;

  // Tipo de zona
  @Column(name = "es_interior")
  @Builder.Default
  private Boolean esInterior = true;

  @Column(name = "es_fumado")
  @Builder.Default
  private Boolean esFumado = false;

  @Column(name = "es_vip")
  @Builder.Default
  private Boolean esVip = false;

  // Capacidad
  @Column(name = "capacidad_maxima")
  private Integer capacidadMaxima;

  // Relación con mesas
  @OneToMany(mappedBy = "zona", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  private Set<Mesa> mesas = new HashSet<>();

  // Métodos helper
  public int cantidadMesas() {
    return mesas.size();
  }

  public int cantidadMesasDisponibles() {
    return (int) mesas.stream()
        .filter(Mesa::estaLibre)
        .count();
  }

  public int cantidadMesasOcupadas() {
    return (int) mesas.stream()
        .filter(Mesa::estaOcupada)
        .count();
  }
}