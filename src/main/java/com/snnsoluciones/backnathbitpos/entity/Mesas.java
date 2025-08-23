package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Mesas {

  @Id
  private Integer id;

  @Column(name = "nombre_mesa")
  private String nombre;

  @Column(name = "numero_mesa")
  private Integer numero;

  @Column(name = "estado")
  private Boolean estado;

  @Column(name = "tipo")
  private String tipo;

}
