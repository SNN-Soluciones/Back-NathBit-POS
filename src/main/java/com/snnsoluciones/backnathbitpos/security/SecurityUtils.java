package com.snnsoluciones.backnathbitpos.security;

import com.snnsoluciones.backnathbitpos.entity.Usuario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * Utilidad para acceder al contexto de seguridad
 * "Nobody calls me chicken!" - Marty McFly
 * Arquitectura La Jachuda 🚀
 */
@Slf4j
public final class SecurityUtils {

    private SecurityUtils() {
        // Utility class
    }

    /**
     * Obtiene el username del usuario actual
     * @return Username o "sistema" si no hay autenticación
     */
    public static String getCurrentUserLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return "sistema";
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        }

        return "sistema";
    }

    /**
     * Obtiene el ID del usuario actual desde el JWT
     * @return ID del usuario o null
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return null;
        }

        // Tu JWT ya incluye el userId en los claims
        // Puedes obtenerlo directamente del token si lo necesitas
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getId();
        }

        return null;
    }

    /**
     * Verifica si el usuario actual tiene un rol específico
     * @param role Rol a verificar (sin ROLE_ prefix)
     * @return true si tiene el rol
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String roleWithPrefix = "ROLE_" + role;
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals(roleWithPrefix));
    }

    /**
     * Verifica si el usuario tiene alguno de los roles especificados
     * @param roles Roles a verificar
     * @return true si tiene al menos uno
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
     * Obtiene la Authentication actual
     * @return Optional con la autenticación
     */
    public static Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Verifica si el usuario está autenticado
     * @return true si está autenticado
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
            && !"anonymousUser".equals(authentication.getName());
    }

    /**
     * Obtiene el usuario completo si usas un custom UserDetails
     * @return Optional con el usuario
     */
    public static Optional<Usuario> getCurrentUser() {
        return getAuthentication()
            .map(Authentication::getPrincipal)
            .filter(principal -> principal instanceof CustomUserDetails)
            .map(principal -> ((CustomUserDetails) principal).getUsuario());
    }

    /**
     * Limpia el contexto de seguridad
     */
    public static void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Verifica si es un usuario del sistema (ROOT o SOPORTE)
     * @return true si es usuario del sistema
     */
    public static boolean isSystemUser() {
        return hasAnyRole("ROOT", "SOPORTE");
    }

    /**
     * Verifica si es un usuario administrativo
     * @return true si puede administrar
     */
    public static boolean isAdmin() {
        return hasAnyRole("ROOT", "SOPORTE", "SUPER_ADMIN", "ADMIN");
    }

    /**
     * Verifica si es un usuario operativo (caja)
     * @return true si es operativo
     */
    public static boolean isOperativo() {
        return hasAnyRole("CAJERO", "JEFE_CAJAS", "MESERO", "COCINERO");
    }
}