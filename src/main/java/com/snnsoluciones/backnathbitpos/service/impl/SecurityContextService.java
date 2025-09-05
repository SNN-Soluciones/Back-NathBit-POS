package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.SesionCaja;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.repository.SesionCajaRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.security.ContextoUsuario;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Servicio para manejar el contexto de seguridad y obtener información
 * del usuario actual y su contexto de empresa/sucursal
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityContextService {

    private final UsuarioRepository usuarioRepository;
    private final SesionCajaRepository sesionCajaRepository;

    /**
     * Obtiene el contexto completo del usuario actual
     */
    public ContextoUsuario getContextoActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Usuario no autenticado");
        }

        // El JwtAuthenticationFilter guarda el ContextoUsuario como principal
        if (authentication.getPrincipal() instanceof ContextoUsuario) {
            return (ContextoUsuario) authentication.getPrincipal();
        }

        throw new UnauthorizedException("Contexto de usuario no válido");
    }

    /**
     * Obtiene el ID del usuario actual desde el SecurityContext
     */
    public Long getCurrentUserId() {
        return getContextoActual().getUserId();
    }

    /**
     * Obtiene el usuario actual completo desde la base de datos
     */
    public Usuario getCurrentUser() {
        Long userId = getCurrentUserId();
        return usuarioRepository.findById(userId)
            .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
    }

    /**
     * Obtiene el email del usuario actual
     */
    public String getCurrentUserEmail() {
        return getContextoActual().getEmail();
    }

    /**
     * Obtiene el rol del usuario actual
     */
    public String getCurrentUserRole() {
        return getContextoActual().getRol();
    }

    /**
     * Obtiene el ID de la empresa del contexto actual
     * @return ID de la empresa o null si no hay contexto de empresa
     */
    public Long getCurrentEmpresaId() {
        return getContextoActual().getEmpresaId();
    }

    /**
     * Obtiene el ID de la sucursal del contexto actual
     * @return ID de la sucursal o null si no hay contexto de sucursal
     */
    public Long getCurrentSucursalId() {
        return getContextoActual().getSucursalId();
    }

    /**
     * Obtiene el ID del terminal actual basado en la sesión de caja activa
     * @return ID del terminal o null si no hay sesión activa
     */
    public Long getCurrentTerminalId() {
        Long userId = getCurrentUserId();
        Long sucursalId = getCurrentSucursalId();

        if (sucursalId == null) {
            log.debug("No hay sucursal en el contexto, no se puede determinar el terminal");
            return null;
        }

        // Buscar sesión de caja activa
        Optional<SesionCaja> sesionActiva = getSesionCajaActiva();

        if (sesionActiva.isPresent()) {
            Long terminalId = sesionActiva.get().getTerminal().getId();
            log.debug("Terminal activo encontrado: {}", terminalId);
            return terminalId;
        }

        log.debug("No hay sesión de caja activa para el usuario");
        return null;
    }

    /**
     * Verifica si el usuario actual es de nivel sistema (ROOT o SOPORTE)
     */
    public boolean isRolSistema() {
        String rol = getCurrentUserRole();
        return "ROOT".equals(rol) || "SOPORTE".equals(rol);
    }

    /**
     * Verifica si el usuario actual tiene una sesión de caja activa
     */
    public Optional<SesionCaja> getSesionCajaActiva() {
        Long userId = getCurrentUserId();
        Long sucursalId = getCurrentSucursalId();

        if (sucursalId == null) {
            return Optional.empty();
        }

        return sesionCajaRepository.findSesionActivaByUsuarioAndSucursal(userId, sucursalId);
    }

    /**
     * Obtiene el request actual si está disponible
     */
    public Optional<HttpServletRequest> getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes)
            RequestContextHolder.getRequestAttributes();

        if (attrs != null) {
            return Optional.of(attrs.getRequest());
        }

        return Optional.empty();
    }

    /**
     * Obtiene un header del request actual
     */
    public Optional<String> getRequestHeader(String headerName) {
        return getCurrentRequest()
            .map(request -> request.getHeader(headerName));
    }

    /**
     * Verifica si el usuario actual tiene alguno de los roles especificados
     * @param roles roles a verificar
     * @return true si tiene al menos uno de los roles
     */
    public boolean hasAnyRole(String... roles) {
        String currentRole = getCurrentUserRole();
        if (currentRole == null) {
            return false;
        }

        for (String role : roles) {
            if (currentRole.equals(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica si el usuario actual tiene el rol especificado
     * @param role rol a verificar
     * @return true si tiene el rol
     */
    public boolean hasRole(String role) {
        String currentRole = getCurrentUserRole();
        return currentRole != null && currentRole.equals(role);
    }

    /**
     * Verifica si el usuario es supervisor (tiene permisos elevados)
     * @return true si es supervisor
     */
    public boolean isSupervisor() {
        return hasAnyRole("JEFE_CAJAS", "ADMIN", "SUPER_ADMIN", "ROOT", "SOPORTE");
    }

    /**
     * Verifica si el usuario es operativo (cajero, mesero, etc)
     * @return true si es operativo
     */
    public boolean isOperativo() {
        return hasAnyRole("CAJERO", "MESERO", "COCINERO", "JEFE_CAJAS");
    }
}