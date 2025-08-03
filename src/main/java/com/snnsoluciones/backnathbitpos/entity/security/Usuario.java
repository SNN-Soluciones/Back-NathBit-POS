package com.snnsoluciones.backnathbitpos.entity.security;

import com.snnsoluciones.backnathbitpos.entity.base.BaseEntity;
import com.snnsoluciones.backnathbitpos.entity.tenant.Sucursal;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario extends BaseEntity implements UserDetails {

  @Column(nullable = false, unique = true, length = 100)
  private String email;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false, length = 50)
  private String nombre;

  @Column(nullable = false, length = 50)
  private String apellidos;

  @Column(length = 20)
  private String telefono;

  @Column(name = "ultimo_acceso")
  private LocalDateTime ultimoAcceso;

  @Column(name = "intentos_fallidos")
  @Builder.Default
  private Integer intentosFallidos = 0;

  @Column(name = "cuenta_bloqueada")
  @Builder.Default
  private Boolean cuentaBloqueada = false;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "usuarios_roles",
      joinColumns = @JoinColumn(name = "usuario_id"),
      inverseJoinColumns = @JoinColumn(name = "rol_id")
  )
  @Builder.Default
  private Set<Rol> roles = new HashSet<>();

  // Métodos de UserDetails
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return roles.stream()
        .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol.getNombre().name()))
        .collect(Collectors.toList());
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return !cuentaBloqueada;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return getActivo() != null && getActivo();
  }

  // Métodos helper
  public void agregarRol(Rol rol) {
    roles.add(rol);
    rol.getUsuarios().add(this);
  }

  public void removerRol(Rol rol) {
    roles.remove(rol);
    rol.getUsuarios().remove(this);
  }

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "usuarios_sucursales",
      joinColumns = @JoinColumn(name = "usuario_id"),
      inverseJoinColumns = @JoinColumn(name = "sucursal_id")
  )
  @Builder.Default
  private Set<Sucursal> sucursales = new HashSet<>();

  // Agregar estos métodos helper
  public void agregarSucursal(Sucursal sucursal) {
    sucursales.add(sucursal);
    sucursal.getUsuarios().add(this);
  }

  public void removerSucursal(Sucursal sucursal) {
    sucursales.remove(sucursal);
    sucursal.getUsuarios().remove(this);
  }

  public boolean tieneSucursal(String codigoSucursal) {
    return sucursales.stream()
        .anyMatch(s -> s.getCodigo().equals(codigoSucursal));
  }
}