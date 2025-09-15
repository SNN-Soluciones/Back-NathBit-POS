// src/main/java/.../entity/Mesa.java
package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.EstadoMesa;
import jakarta.persistence.*;
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
  private Integer capacidad = 2;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private EstadoMesa estado = EstadoMesa.LIBRE;

  @Column(nullable = false)
  private Boolean activo = true;

  @Column(nullable = false)
  private Integer orden = 0;

  // Para unir mesas (misma orden), agrupa lógicamente varias mesas
  @Column(name = "union_group_id")
  private Long unionGroupId;
}