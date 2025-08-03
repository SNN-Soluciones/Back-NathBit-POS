package com.snnsoluciones.backnathbitpos.entity.security;

import com.snnsoluciones.backnathbitpos.entity.base.BaseEntity;
import com.snnsoluciones.backnathbitpos.entity.operacion.Caja;
import com.snnsoluciones.backnathbitpos.entity.tenant.Sucursal;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
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
@SuperBuilder
public class Usuario extends BaseEntity implements UserDetails {

  @Column(nullable = false, length = 255)
  private String email;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false, length = 100)
  private String nombre;

  @Column(length = 100)
  private String apellidos;

  @Column(length = 50)
  private String telefono;

  @Column(length = 50)
  private String identificacion;

  @Column(name = "tipo_identificacion", length = 20)
  private String tipoIdentificacion;

  @Column(name = "ultimo_acceso")
  private LocalDateTime ultimoAcceso;

  @Column(name = "intentos_fallidos")
  @Builder.Default
  private Integer intentosFallidos = 0;

  @Column(name = "bloqueado")
  @Builder.Default
  private Boolean bloqueado = false;

  @Column(name = "forzar_reloging")
  private Boolean forzarRelogin;

  // Relación con Rol (uno a muchos según la BD)
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "rol_id")
  private Rol rol;

  // Relación con Sucursal predeterminada
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sucursal_predeterminada_id")
  private Sucursal sucursalPredeterminada;

  // Relación muchos a muchos con Sucursales
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "usuario_sucursales",
      joinColumns = @JoinColumn(name = "usuario_id"),
      inverseJoinColumns = @JoinColumn(name = "sucursal_id")
  )
  @Builder.Default
  private Set<Sucursal> sucursales = new HashSet<>();

  // Relación muchos a muchos con Cajas
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "usuario_cajas",
      joinColumns = @JoinColumn(name = "usuario_id"),
      inverseJoinColumns = @JoinColumn(name = "caja_id")
  )
  @Builder.Default
  private Set<Caja> cajas = new HashSet<>();

  @Transient
  private Set<SimpleGrantedAuthority> authorities;

  // Relación con múltiples tenants
  @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
  private Set<UsuarioTenant> tenantMemberships = new HashSet<>();


  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    // Si hay authorities personalizadas (pre-auth), usarlas
    if (authorities != null && !authorities.isEmpty()) {
      return authorities;
    }

    // Para operaciones normales con tenant
    Set<GrantedAuthority> allAuthorities = new HashSet<>();

    // Si hay un rol directo (compatibilidad hacia atrás)
    if (rol != null) {
      allAuthorities.add(new SimpleGrantedAuthority("ROLE_" + rol.getNombre().name()));
      if (rol.getPermisos() != null) {
        rol.getPermisos().forEach(p ->
            allAuthorities.add(new SimpleGrantedAuthority(p.getCodigo()))
        );
      }
    }

    // Agregar authority especial si es owner de algún tenant
    if (tenantMemberships != null) {
      boolean isOwnerAnywhere = tenantMemberships.stream()
          .anyMatch(UsuarioTenant::isEsPropietario);

      if (isOwnerAnywhere) {
        allAuthorities.add(new SimpleGrantedAuthority("MULTI_TENANT_OWNER"));
      }

      // Si tiene acceso a múltiples tenants
      if (tenantMemberships.size() > 1) {
        allAuthorities.add(new SimpleGrantedAuthority("MULTI_TENANT_ACCESS"));
      }
    }

    return allAuthorities;
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
    return !bloqueado;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return Boolean.TRUE.equals(getActivo());
  }

  // Métodos helper
  public String getNombreCompleto() {
    return nombre + " " + (apellidos != null ? apellidos : "");
  }

  public boolean tieneRol(String nombreRol) {
    return rol != null && rol.getNombre().name().equals(nombreRol);
  }

  public boolean tienePermiso(String codigoPermiso) {
    if (rol == null || rol.getPermisos() == null) {
      return false;
    }
    return rol.getPermisos().stream()
        .anyMatch(permiso -> permiso.getCodigo().equals(codigoPermiso));
  }

  // Método helper para obtener rol en tenant específico
  public Rol getRolEnTenant(String tenantId) {
    return tenantMemberships.stream()
        .filter(ut -> ut.getTenantId().equals(tenantId))
        .map(UsuarioTenant::getRol)
        .findFirst()
        .orElse(null);
  }

  // Verificar si es owner en un tenant específico
  public boolean isOwnerInTenant(String tenantId) {
    return tenantMemberships.stream()
        .anyMatch(ut -> ut.getTenantId().equals(tenantId) && ut.isEsPropietario());
  }
}