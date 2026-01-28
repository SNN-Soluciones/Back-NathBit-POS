// src/main/java/.../entity/Mesa.java
package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.EstadoMesa;
import com.snnsoluciones.backnathbitpos.enums.TipoFormaMesa;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "mesa",
  uniqueConstraints = @UniqueConstraint(name = "uk_mesa_codigo_zona", columnNames = {"zona_id", "codigo"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Mesa {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "zona_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mesa_zona"))
  private ZonaMesa zona;

  // Código visible para staff (p.ej: A1, B3)
  @Column(name = "numero_mesa", nullable = false, length = 20)
  private String codigo;

  @Column(length = 60)
  private String nombre; // opcional (alias)

  @Column(nullable = false)
  @Builder.Default
  private Integer capacidad = 2;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  @Builder.Default
  private EstadoMesa estado = EstadoMesa.DISPONIBLE;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "sucursal_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mesa_sucursal"))
  private Sucursal sucursal;

  @Column(nullable = false)
  @Builder.Default
  private Boolean activo = true;

  @Column(nullable = false)
  @Builder.Default
  private Integer orden = 0;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_forma", nullable = false, length = 30)
  @Builder.Default
  private TipoFormaMesa tipoForma = TipoFormaMesa.CUADRADA;

  @OneToMany(mappedBy = "mesa")
  @Builder.Default
  private List<Orden> ordenes = new ArrayList<>();

  // Para unir mesas (misma orden), agrupa lógicamente varias mesas
  @Column(name = "union_group_id")
  private Long unionGroupId;

  @Transient
  public Orden getOrdenActiva() {
    return ordenes.stream()
        .filter(o -> !o.getEstado().esFinal())
        .findFirst()
        .orElse(null);
  }

  @Transient
  public boolean tieneOrdenActiva() {
    return getOrdenActiva() != null;
  }

  @Transient
  public BigDecimal getTotalOrdenActiva() {
    Orden ordenActiva = getOrdenActiva();
    return ordenActiva != null ? ordenActiva.getTotal() : BigDecimal.ZERO;
  }

  // Método helper para cambiar estado basado en órdenes
  public void actualizarEstadoSegunOrden() {
    if (tieneOrdenActiva()) {
      if (this.estado != EstadoMesa.OCUPADA) {
        this.estado = EstadoMesa.OCUPADA;
      }
    } else {
      if (this.estado == EstadoMesa.OCUPADA) {
        this.estado = EstadoMesa.DISPONIBLE;
      }
    }
  }
}