// Archivo: src/main/java/com/snnsoluciones/backnathbitpos/entity/operacion/Caja.java

package com.snnsoluciones.backnathbitpos.entity.operacion;

import com.snnsoluciones.backnathbitpos.entity.base.BaseEntity;
import com.snnsoluciones.backnathbitpos.entity.security.Usuario;
import com.snnsoluciones.backnathbitpos.entity.tenant.Sucursal;
import com.snnsoluciones.backnathbitpos.enums.EstadoCaja;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "cajas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Caja extends BaseEntity {

  @Column(nullable = false, unique = true, length = 20)
  private String codigo;

  @Column(nullable = false, length = 100)
  private String nombre;

  @Column(length = 200)
  private String descripcion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sucursal_id", nullable = false)
  private Sucursal sucursal;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private EstadoCaja estado = EstadoCaja.CERRADA;

  // Información de apertura/cierre actual
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "usuario_apertura_id")
  private Usuario usuarioApertura;

  @Column(name = "fecha_apertura")
  private LocalDateTime fechaApertura;

  @Column(name = "monto_apertura", precision = 18, scale = 2)
  private BigDecimal montoApertura;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "usuario_cierre_id")
  private Usuario usuarioCierre;

  @Column(name = "fecha_cierre")
  private LocalDateTime fechaCierre;

  @Column(name = "monto_cierre", precision = 18, scale = 2)
  private BigDecimal montoCierre;

  // Terminal o punto de venta
  @Column(name = "numero_terminal", nullable = false, length = 5)
  @Builder.Default
  private String numeroTerminal = "00001";

  // Usuarios autorizados para usar esta caja
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "cajas_usuarios",
      joinColumns = @JoinColumn(name = "caja_id"),
      inverseJoinColumns = @JoinColumn(name = "usuario_id")
  )
  @Builder.Default
  private Set<Usuario> usuariosAutorizados = new HashSet<>();

  // Métodos helper
  public boolean estaAbierta() {
    return EstadoCaja.ABIERTA.equals(estado);
  }

  public boolean estaCerrada() {
    return EstadoCaja.CERRADA.equals(estado);
  }

  public void abrir(Usuario usuario, BigDecimal montoInicial) {
    this.estado = EstadoCaja.ABIERTA;
    this.usuarioApertura = usuario;
    this.fechaApertura = LocalDateTime.now();
    this.montoApertura = montoInicial;
    this.usuarioCierre = null;
    this.fechaCierre = null;
    this.montoCierre = null;
  }

  public void cerrar(Usuario usuario, BigDecimal montoFinal) {
    this.estado = EstadoCaja.CERRADA;
    this.usuarioCierre = usuario;
    this.fechaCierre = LocalDateTime.now();
    this.montoCierre = montoFinal;
  }
}