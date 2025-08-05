// Archivo: src/main/java/com/snnsoluciones/backnathbitpos/entity/operacion/Mesa.java

package com.snnsoluciones.backnathbitpos.entity.operacion;

import com.snnsoluciones.backnathbitpos.entity.base.BaseEntity;
import com.snnsoluciones.backnathbitpos.entity.global.UsuarioGlobal;
import com.snnsoluciones.backnathbitpos.entity.tenant.Sucursal;
import com.snnsoluciones.backnathbitpos.enums.EstadoMesa;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "mesas",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"sucursal_id", "numero"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mesa extends BaseEntity {

  @Column(nullable = false, length = 20)
  private String numero;

  @Column(nullable = false, length = 100)
  private String nombre;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sucursal_id", nullable = false)
  private Sucursal sucursal;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "zona_id", nullable = false)
  private Zona zona;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private EstadoMesa estado = EstadoMesa.LIBRE;

  // Capacidad y configuración
  @Column(name = "capacidad_personas", nullable = false)
  @Builder.Default
  private Integer capacidadPersonas = 4;

  @Column(name = "es_unible")
  @Builder.Default
  private Boolean esUnible = true;

  @Column(name = "es_activa")
  @Builder.Default
  private Boolean esActiva = true;

  // Información de ocupación actual
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "mesero_asignado_id")
  private UsuarioGlobal meseroAsignado;

  @Column(name = "fecha_apertura")
  private LocalDateTime fechaApertura;

  @Column(name = "cantidad_comensales")
  private Integer cantidadComensales;

  @Column(name = "nombre_cliente", length = 100)
  private String nombreCliente;

  @Column(name = "observaciones", columnDefinition = "TEXT")
  private String observaciones;

  // Posición visual (para UI)
  @Column(name = "posicion_x")
  private Integer posicionX;

  @Column(name = "posicion_y")
  private Integer posicionY;

  @Column(name = "forma", length = 20)
  @Builder.Default
  private String forma = "CUADRADA"; // CUADRADA, REDONDA, RECTANGULAR

  // Relaciones
  @OneToMany(mappedBy = "mesa", fetch = FetchType.LAZY)
  @Builder.Default
  private Set<Orden> ordenes = new HashSet<>();

  // Mesas unidas (para grupos grandes)
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "mesas_unidas",
      joinColumns = @JoinColumn(name = "mesa_id"),
      inverseJoinColumns = @JoinColumn(name = "mesa_unida_id")
  )
  @Builder.Default
  private Set<Mesa> mesasUnidas = new HashSet<>();

  // Métodos helper
  public boolean estaLibre() {
    return EstadoMesa.LIBRE.equals(estado);
  }

  public boolean estaOcupada() {
    return EstadoMesa.OCUPADA.equals(estado);
  }

  public boolean estaReservada() {
    return EstadoMesa.RESERVADA.equals(estado);
  }

  public boolean estaBloqueada() {
    return EstadoMesa.BLOQUEADA.equals(estado);
  }

  public void ocupar(UsuarioGlobal mesero, Integer comensales, String cliente) {
    this.estado = EstadoMesa.OCUPADA;
    this.meseroAsignado = mesero;
    this.fechaApertura = LocalDateTime.now();
    this.cantidadComensales = comensales;
    this.nombreCliente = cliente;
  }

  public void liberar() {
    this.estado = EstadoMesa.LIBRE;
    this.meseroAsignado = null;
    this.fechaApertura = null;
    this.cantidadComensales = null;
    this.nombreCliente = null;
    this.observaciones = null;
    this.mesasUnidas.clear();
  }

  public void reservar(String cliente, String observaciones) {
    this.estado = EstadoMesa.RESERVADA;
    this.nombreCliente = cliente;
    this.observaciones = observaciones;
  }

  public void bloquear(String motivo) {
    this.estado = EstadoMesa.BLOQUEADA;
    this.observaciones = motivo;
  }

  public void unirCon(Mesa otraMesa) {
    this.mesasUnidas.add(otraMesa);
    otraMesa.getMesasUnidas().add(this);
  }

  public void desunir() {
    for (Mesa mesa : mesasUnidas) {
      mesa.getMesasUnidas().remove(this);
    }
    this.mesasUnidas.clear();
  }

  public int getCapacidadTotal() {
    int capacidad = this.capacidadPersonas;
    for (Mesa mesa : mesasUnidas) {
      capacidad += mesa.getCapacidadPersonas();
    }
    return capacidad;
  }
}