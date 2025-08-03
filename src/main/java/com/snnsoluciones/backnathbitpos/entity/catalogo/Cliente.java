// Archivo: src/main/java/com/snnsoluciones/backnathbitpos/entity/catalogo/Cliente.java

package com.snnsoluciones.backnathbitpos.entity.catalogo;

import com.snnsoluciones.backnathbitpos.entity.base.BaseEntity;
import com.snnsoluciones.backnathbitpos.enums.TipoIdentificacion;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "clientes",
    indexes = {
        @Index(name = "idx_cliente_identificacion", columnList = "numero_identificacion"),
        @Index(name = "idx_cliente_email", columnList = "email")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente extends BaseEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_identificacion", nullable = false, length = 20)
  private TipoIdentificacion tipoIdentificacion;

  @Column(name = "numero_identificacion", nullable = false, unique = true, length = 20)
  private String numeroIdentificacion;

  @Column(nullable = false, length = 100)
  private String nombre;

  @Column(length = 100)
  private String apellidos;

  @Column(name = "nombre_comercial", length = 200)
  private String nombreComercial;

  @Column(length = 100)
  private String email;

  @Column(length = 20)
  private String telefono;

  @Column(name = "telefono_movil", length = 20)
  private String telefonoMovil;

  // Dirección
  @Column(length = 1)
  private String provincia;

  @Column(length = 2)
  private String canton;

  @Column(length = 2)
  private String distrito;

  @Column(length = 50)
  private String barrio;

  @Column(name = "otras_senas", columnDefinition = "TEXT")
  private String otrasSenas;

  // Información adicional
  @Column(name = "fecha_nacimiento")
  private LocalDate fechaNacimiento;

  @Column(name = "es_contribuyente")
  @Builder.Default
  private Boolean esContribuyente = false;

  @Column(name = "es_exonerado")
  @Builder.Default
  private Boolean esExonerado = false;

  @Column(name = "numero_exoneracion", length = 50)
  private String numeroExoneracion;

  // Crédito
  @Column(name = "tiene_credito")
  @Builder.Default
  private Boolean tieneCredito = false;

  @Column(name = "limite_credito", precision = 18, scale = 2)
  @Builder.Default
  private BigDecimal limiteCredito = BigDecimal.ZERO;

  @Column(name = "saldo_credito", precision = 18, scale = 2)
  @Builder.Default
  private BigDecimal saldoCredito = BigDecimal.ZERO;

  @Column(name = "dias_credito")
  @Builder.Default
  private Integer diasCredito = 0;

  // Marketing
  @Column(name = "acepta_publicidad")
  @Builder.Default
  private Boolean aceptaPublicidad = true;

  @Column(name = "es_vip")
  @Builder.Default
  private Boolean esVip = false;

  @Column(columnDefinition = "TEXT")
  private String observaciones;

  // Métodos helper
  public String getNombreCompleto() {
    if (apellidos != null && !apellidos.isEmpty()) {
      return nombre + " " + apellidos;
    }
    return nombre;
  }

  public BigDecimal getCreditoDisponible() {
    return limiteCredito.subtract(saldoCredito);
  }

  public boolean tieneCreditoDisponible() {
    return tieneCredito && getCreditoDisponible().compareTo(BigDecimal.ZERO) > 0;
  }
}