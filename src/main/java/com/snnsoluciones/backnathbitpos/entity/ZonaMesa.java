// src/main/java/.../entity/ZonaMesa.java
package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "zona_mesa",
  uniqueConstraints = @UniqueConstraint(name = "uk_zona_nombre_sucursal", columnNames = {"sucursal_id", "nombre"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ZonaMesa {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "sucursal_id", nullable = false, foreignKey = @ForeignKey(name = "fk_zona_sucursal"))
  private Sucursal sucursal;

  @Column(nullable = false, length = 60)
  private String nombre;

  @Column(length = 200)
  private String descripcion;

  @Column(nullable = false)
  @Builder.Default
  private Boolean activo = true;

  @Column(name = "orden_exhibicion", nullable = false)
  @Builder.Default
  private Integer ordenExhibicion = 1;
}