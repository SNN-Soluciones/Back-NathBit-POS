// Archivo: src/main/java/com/snnsoluciones/backnathbitpos/entity/tenant/Sucursal.java

package com.snnsoluciones.backnathbitpos.entity.tenant;

import com.snnsoluciones.backnathbitpos.entity.base.BaseEntity;
import com.snnsoluciones.backnathbitpos.entity.operacion.Caja;
import com.snnsoluciones.backnathbitpos.entity.operacion.Mesa;
import com.snnsoluciones.backnathbitpos.entity.operacion.Zona;
import com.snnsoluciones.backnathbitpos.entity.security.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sucursales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sucursal extends BaseEntity {

  @Column(nullable = false, unique = true, length = 10)
  private String codigo;

  @Column(nullable = false, length = 200)
  private String nombre;

  @Column(name = "nombre_comercial", length = 200)
  private String nombreComercial;

  @Column(length = 20)
  private String telefono;

  @Column(nullable = false, length = 100)
  private String email;

  // Dirección
  @Column(nullable = false, length = 1)
  private String provincia;

  @Column(nullable = false, length = 2)
  private String canton;

  @Column(nullable = false, length = 2)
  private String distrito;

  @Column(length = 50)
  private String barrio;

  @Column(name = "otras_senas", columnDefinition = "TEXT")
  private String otrasSenas;

  // Relación con Tenant (en schema public)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
  private Tenant tenant;

  // Configuración fiscal
  @Column(name = "numero_caja_registradora", length = 5)
  @Builder.Default
  private String numeroCajaRegistradora = "00001";

  @Column(name = "consecutivo_factura")
  @Builder.Default
  private Long consecutivoFactura = 1L;

  @Column(name = "consecutivo_tiquete")
  @Builder.Default
  private Long consecutivoTiquete = 1L;

  @Column(name = "consecutivo_nc")
  @Builder.Default
  private Long consecutivoNC = 1L;

  @Column(name = "consecutivo_nd")
  @Builder.Default
  private Long consecutivoND = 1L;

  @Column(name = "consecutivo_mensaje")
  @Builder.Default
  private Long consecutivoMensaje = 1L;

  // Configuración adicional
  @Column(columnDefinition = "JSONB")
  @Builder.Default
  private String configuracion = "{}";

  @Column(name = "es_principal")
  @Builder.Default
  private Boolean esPrincipal = false;

  // Relaciones
  @OneToMany(mappedBy = "sucursal", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  private Set<Caja> cajas = new HashSet<>();

  @OneToMany(mappedBy = "sucursal", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  private Set<Zona> zonas = new HashSet<>();

  @OneToMany(mappedBy = "sucursal", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  private Set<Mesa> mesas = new HashSet<>();

  @ManyToMany(mappedBy = "sucursales", fetch = FetchType.LAZY)
  @Builder.Default
  private Set<Usuario> usuarios = new HashSet<>();

  // Métodos helper
  public String obtenerCodigoCompleto() {
    return String.format("%03d", Integer.parseInt(codigo));
  }

  public synchronized Long siguienteConsecutivoFactura() {
    return ++consecutivoFactura;
  }

  public synchronized Long siguienteConsecutivoTiquete() {
    return ++consecutivoTiquete;
  }

  public synchronized Long siguienteConsecutivoNC() {
    return ++consecutivoNC;
  }

  public synchronized Long siguienteConsecutivoND() {
    return ++consecutivoND;
  }

  public synchronized Long siguienteConsecutivoMensaje() {
    return ++consecutivoMensaje;
  }
}