package com.snnsoluciones.backnathbitpos.config.security;

import com.snnsoluciones.backnathbitpos.config.tenant.TenantContext;
import com.snnsoluciones.backnathbitpos.entity.security.Permiso;
import com.snnsoluciones.backnathbitpos.entity.security.Rol;
import com.snnsoluciones.backnathbitpos.entity.security.Usuario;
import com.snnsoluciones.backnathbitpos.entity.security.UsuarioTenant;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.exception.TenantException;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioTenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

  private final UsuarioRepository usuarioRepository;
  private final UsuarioTenantRepository usuarioTenantRepository;

  @Override
  @Transactional
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    String tenantId = TenantContext.getCurrentTenant();

    log.debug("Cargando usuario: {} con tenant: {}", email, tenantId);

    // Buscar usuario por email
    Usuario usuario = usuarioRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException(
            "Usuario no encontrado con email: " + email
        ));

    // Verificar si el usuario está activo
    if (!usuario.getActivo()) {
      throw new UsernameNotFoundException("Usuario inactivo: " + email);
    }

    // Si no hay tenant en el contexto (login inicial o token sin tenant)
    if (tenantId == null) {
      log.debug("Cargando usuario {} sin tenant específico (login inicial)", email);
      return createUserDetailsForInitialAuth(usuario);
    } else {
      // Operaciones normales con tenant
      log.debug("Cargando usuario {} para tenant {}", email, tenantId);
      return createUserDetailsForTenant(usuario, tenantId);
    }
  }

  /**
   * Crea UserDetails para autenticación inicial (sin tenant seleccionado)
   */
  private UserDetails createUserDetailsForInitialAuth(Usuario usuario) {
    // Para el login inicial, crear un usuario con permisos mínimos
    // Solo permitir operaciones de consulta de tenants y selección
    Set<SimpleGrantedAuthority> authorities = new HashSet<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_PRE_AUTH"));
    authorities.add(new SimpleGrantedAuthority("AUTH:SELECT_TENANT"));
    authorities.add(new SimpleGrantedAuthority("AUTH:VIEW_TENANTS"));

    // Crear una copia del usuario sin tenant específico
    Usuario userForAuth = new Usuario();
    userForAuth.setId(usuario.getId());
    userForAuth.setEmail(usuario.getEmail());
    userForAuth.setPassword(usuario.getPassword());
    userForAuth.setNombre(usuario.getNombre());
    userForAuth.setApellidos(usuario.getApellidos());
    userForAuth.setActivo(usuario.getActivo());
    userForAuth.setBloqueado(usuario.getBloqueado());
    userForAuth.setAuthorities(authorities);

    // No establecer tenant ni rol específico
    userForAuth.setTenantId(null);
    userForAuth.setRol(null);

    return userForAuth;
  }

  /**
   * Crea UserDetails para operaciones con tenant específico
   */
  private UserDetails createUserDetailsForTenant(Usuario usuario, String tenantId) {
    // Buscar la relación usuario-tenant
    UsuarioTenant usuarioTenant = usuarioTenantRepository
        .findByUsuarioIdAndTenantIdAndActivo(usuario.getId(), tenantId, true)
        .orElseThrow(() -> new TenantException(
            "Usuario no tiene acceso al tenant: " + tenantId, tenantId
        ));

    // Obtener el rol específico para este tenant
    Rol rolTenant = usuarioTenant.getRol();
    if (rolTenant == null) {
      throw new TenantException("Usuario no tiene rol asignado en el tenant: " + tenantId, tenantId);
    }

    // Establecer el rol y tenant en el usuario
    usuario.setRol(rolTenant);
    usuario.setTenantId(tenantId);

    // Construir authorities basadas en el rol del tenant
    Set<SimpleGrantedAuthority> authorities = buildAuthorities(rolTenant);
    usuario.setAuthorities(authorities);

    log.debug("Usuario {} cargado con rol {} para tenant {}",
        usuario.getEmail(), rolTenant.getNombre(), tenantId);

    return usuario;
  }

  /**
   * Construye las authorities basadas en el rol y sus permisos
   */
  private Set<SimpleGrantedAuthority> buildAuthorities(Rol rol) {
    Set<SimpleGrantedAuthority> authorities = new HashSet<>();

    // Agregar el rol
    authorities.add(new SimpleGrantedAuthority("ROLE_" + rol.getNombre().name()));

    // Agregar permisos del rol
    if (rol.getPermisos() != null) {
      authorities.addAll(
          rol.getPermisos().stream()
              .filter(Permiso::getActivo)
              .map(permiso -> new SimpleGrantedAuthority(permiso.getNombre()))
              .collect(Collectors.toSet())
      );
    }

    // Si es SUPER_ADMIN, agregar permiso especial
    if (rol.getNombre() == RolNombre.SUPER_ADMIN) {
      authorities.add(new SimpleGrantedAuthority("SUPER_ADMIN"));
      authorities.add(new SimpleGrantedAuthority("MANAGE_ALL_TENANTS"));
    }

    return authorities;
  }

  /**
   * Método auxiliar para cargar usuario por email sin verificar tenant
   * Útil para operaciones de super admin
   */
  @Transactional
  public UserDetails loadUserByUsernameIgnoreTenant(String email) throws UsernameNotFoundException {
    Usuario usuario = usuarioRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException(
            "Usuario no encontrado con email: " + email
        ));

    // Para super admin, cargar con todos los permisos
    if (usuario.getRol() != null && usuario.getRol().getNombre() == RolNombre.SUPER_ADMIN) {
      Set<SimpleGrantedAuthority> authorities = buildAuthorities(usuario.getRol());
      usuario.setAuthorities(authorities);
    }

    return usuario;
  }

  /**
   * Verifica si un usuario tiene acceso a un tenant específico
   */
  public boolean hasAccessToTenant(String email, String tenantId) {
    try {
      Usuario usuario = usuarioRepository.findByEmail(email)
          .orElse(null);

      if (usuario == null) return false;

      return usuarioTenantRepository
          .findByUsuarioIdAndTenantIdAndActivo(usuario.getId(), tenantId, true)
          .isPresent();
    } catch (Exception e) {
      log.error("Error verificando acceso al tenant", e);
      return false;
    }
  }
}