package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cliente_exoneracion_cabys",
  uniqueConstraints = @UniqueConstraint(name = "uq_exo_cabys", columnNames = {"exoneracion_id", "cabys_id"}),
  indexes = {
    @Index(name = "idx_exo_id", columnList = "exoneracion_id"),
    @Index(name = "idx_cabys_id", columnList = "cabys_id")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ClienteExoneracionCabys {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "exoneracion_id", nullable = false,
              foreignKey = @ForeignKey(name = "fk_exo_cabys_exo"))
  private ClienteExoneracion exoneracion;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cabys_id", nullable = false,
              foreignKey = @ForeignKey(name = "fk_exo_cabys_cabys"))
  private CodigoCAByS cabys;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}