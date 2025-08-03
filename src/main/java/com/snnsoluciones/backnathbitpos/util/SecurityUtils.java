package com.snnsoluciones.backnathbitpos.util;

import com.snnsoluciones.backnathbitpos.entity.security.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * Clase de utilidades para operaciones de seguridad comunes.
 */
public class SecurityUtils {

  private SecurityUtils() {
    // Constructor privado para evitar instanciación
  }

  /**
   * Obtiene el username del usuario autenticado actual.
   */
  public static Optional<String> getCurrentUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      return Optional.empty();
    }

    Object principal = authentication.getPrincipal();

    if (principal instanceof UserDetails) {
      return Optional.of(((UserDetails) principal).getUsername());
    } else if (principal instanceof String) {
      return Optional.of((String) principal);
    }

    return Optional.empty();
  }

  /**
   * Obtiene el usuario autenticado actual.
   */
  public static Optional<Usuario> getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      return Optional.empty();
    }

    Object principal = authentication.getPrincipal();

    if (principal instanceof Usuario) {
      return Optional.of((Usuario) principal);
    }

    return Optional.empty();
  }

  /**
   * Verifica si el usuario actual está autenticado.
   */
  public static boolean isAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null &&
        authentication.isAuthenticated() &&
        !"anonymousUser".equals(authentication.getPrincipal());
  }

  /**
   * Verifica si el usuario actual tiene un rol específico.
   */
  public static boolean hasRole(String role) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }

    return authentication.getAuthorities().stream()
        .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role) ||
            authority.getAuthority().equals(role));
  }

  /**
   * Verifica si el usuario actual tiene alguno de los roles especificados.
   */
  public static boolean hasAnyRole(String... roles) {
    for (String role : roles) {
      if (hasRole(role)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Verifica si el usuario actual tiene todos los roles especificados.
   */
  public static boolean hasAllRoles(String... roles) {
    for (String role : roles) {
      if (!hasRole(role)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Verifica si el usuario actual tiene un permiso específico.
   */
  public static boolean hasAuthority(String authority) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }

    return authentication.getAuthorities().stream()
        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
  }

  /**
   * Obtiene el tenant ID del usuario actual.
   */
  public static Optional<String> getCurrentTenantId() {
    return getCurrentUser().map(Usuario::getTenantId);
  }

  /**
   * Limpia el contexto de seguridad.
   */
  public static void clearContext() {
    SecurityContextHolder.clearContext();
  }
}