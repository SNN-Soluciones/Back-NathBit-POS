// Archivo: src/main/java/com/snnsoluciones/backnathbitpos/entity/tenant/Tenant.java

package com.snnsoluciones.backnathbitpos.entity.tenant;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tenants", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 50)
  private String codigo;

  @Column(nullable = false, length = 200)
  private String nombre;

  @Column(name = "nombre_comercial", length = 200)
  private String nombreComercial;

  @Column(nullable = false, length = 20)
  private String ruc;

  @Column(nullable = false, length = 100)
  private String email;

  @Column(length = 20)
  private String telefono;

  @Column(columnDefinition = "TEXT")
  private String direccion;

  @Column(name = "schema_name", nullable = false, unique = true, length = 50)
  private String schemaName;

  @Column(length = 50)
  @Builder.Default
  private String plan = "BASICO";

  @Column(name = "fecha_vencimiento_plan")
  private LocalDateTime fechaVencimientoPlan;

  @Column(nullable = false)
  @Builder.Default
  private Boolean activo = true;

  @Column(columnDefinition = "JSONB")
  @Builder.Default
  private String configuracion = "{}";

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  private Set<Sucursal> sucursales = new HashSet<>();
}