// src/main/java/.../entity/MesaEstadoHist.java
package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.EstadoMesa;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "mesa_estado_hist", indexes = {
  @Index(name = "ix_hist_mesa_fecha", columnList = "mesa_id, fecha_cambio desc")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MesaEstadoHist {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "mesa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_hist_mesa"))
  private Mesa mesa;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private EstadoMesa estado;

  @Column(length = 160)
  private String motivo;

  @Column(name = "usuario_id")
  private Long usuarioId; // si manejas usuarios/operadores

  @Column(name = "fecha_cambio", nullable = false)
  private OffsetDateTime fechaCambio;
}