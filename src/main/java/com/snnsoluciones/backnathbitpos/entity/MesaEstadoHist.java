// MesaEstadoHist.java - CORREGIDO
package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;

@Entity
@Table(name = "mesa_estado_hist")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MesaEstadoHist {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "mesa_id", nullable = false)
  private Mesa mesa;

  @Column(name = "estado_anterior")
  private String estadoAnterior;

  @Column(name = "estado_nuevo", nullable = false)
  private String estadoNuevo;

  @Column(name = "usuario_id")
  private Long usuarioId;

  @Column(name = "orden_id")
  private Long ordenId;

  @Column(name = "observacion")
  private String observacion;

  @CreationTimestamp
  @Column(name = "created_at")
  private OffsetDateTime createdAt;
}