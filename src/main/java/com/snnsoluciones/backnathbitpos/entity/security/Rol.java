package com.snnsoluciones.backnathbitpos.entity.security;

import com.snnsoluciones.backnathbitpos.entity.base.BaseEntity;
import com.snnsoluciones.backnathbitpos.entity.global.UsuarioGlobal;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Rol extends BaseEntity {

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, unique = true, length = 30)
  private RolNombre nombre;

  @Column(length = 100)
  private String descripcion;

  // Cambiado de @ManyToMany a @OneToMany porque un usuario tiene un solo rol
  @OneToMany(mappedBy = "rol", fetch = FetchType.LAZY)
  @Builder.Default
  private Set<UsuarioGlobal> usuarios = new HashSet<>();

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "roles_permisos",
      joinColumns = @JoinColumn(name = "rol_id"),
      inverseJoinColumns = @JoinColumn(name = "permiso_id")
  )
  @Builder.Default
  private Set<Permiso> permisos = new HashSet<>();

  // Constructor conveniente
  public Rol(RolNombre nombre) {
    super();
    this.nombre = nombre;
  }
}