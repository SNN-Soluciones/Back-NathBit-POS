// SesionCajaDenominacion.java
package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "sesion_caja_denominacion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SesionCajaDenominacion {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "sesion_caja_id")
  private SesionCaja sesionCaja;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal valor;

  @Column(nullable = false)
  private Integer cantidad;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal total; // valor * cantidad
}