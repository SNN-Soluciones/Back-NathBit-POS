package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.SesionCaja;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.repository.SesionCajaRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
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
     * Obtiene el ID del usuario actual desde el SecurityContext
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Usuario no autenticado");
        }
        
        // El JwtAuthenticationFilter guarda el userId como principal
        if (authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        
        throw new UnauthorizedException("No se pudo obtener el ID del usuario");
    }
    
    /**
     * Obtiene el usuario actual completo
     */
    public Usuario getCurrentUser() {
        Long userId = getCurrentUserId();
        return usuarioRepository.findById(userId)
            .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
    }
    
    /**
     * Obtiene el rol del usuario actual
     */
    public String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getAuthorities() != null) {
            return authentication.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .orElse(null);
        }
        
        return null;
    }
    
    /**
     * Obtiene el ID de la empresa desde los headers HTTP
     */
    public Long getCurrentEmpresaId() {
        HttpServletRequest request = getCurrentHttpRequest();
        String empresaId = request.getHeader("X-Empresa-Id");
        
        if (empresaId == null || empresaId.isEmpty()) {
            // Si no viene en headers, buscar la sesión de caja activa
            return getEmpresaIdFromSesionCaja().orElse(null);
        }
        
        try {
            return Long.parseLong(empresaId);
        } catch (NumberFormatException e) {
            log.error("ID de empresa inválido: {}", empresaId);
            return null;
        }
    }
    
    /**
     * Obtiene el ID de la sucursal desde los headers HTTP
     */
    public Long getCurrentSucursalId() {
        HttpServletRequest request = getCurrentHttpRequest();
        String sucursalId = request.getHeader("X-Sucursal-Id");
        
        if (sucursalId == null || sucursalId.isEmpty()) {
            // Si no viene en headers, buscar la sesión de caja activa
            return getSucursalIdFromSesionCaja().orElse(null);
        }
        
        try {
            return Long.parseLong(sucursalId);
        } catch (NumberFormatException e) {
            log.error("ID de sucursal inválido: {}", sucursalId);
            return null;
        }
    }
    
    /**
     * Obtiene el ID del terminal desde los headers HTTP o sesión de caja
     */
    public Long getCurrentTerminalId() {
        HttpServletRequest request = getCurrentHttpRequest();
        String terminalId = request.getHeader("X-Terminal-Id");
        
        if (terminalId == null || terminalId.isEmpty()) {
            // Si no viene en headers, buscar la sesión de caja activa
            return getTerminalIdFromSesionCaja().orElse(null);
        }
        
        try {
            return Long.parseLong(terminalId);
        } catch (NumberFormatException e) {
            log.error("ID de terminal inválido: {}", terminalId);
            return null;
        }
    }
    
    /**
     * Verifica si el usuario actual tiene alguno de los roles especificados
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
     * Verifica si el usuario actual tiene un rol específico
     */
    public boolean hasRole(String role) {
        String currentRole = getCurrentUserRole();
        return currentRole != null && currentRole.equals(role);
    }
    
    /**
     * Obtiene la sesión de caja activa del usuario
     */
    public Optional<SesionCaja> getCurrentSesionCaja() {
        Long userId = getCurrentUserId();
        return sesionCajaRepository.findActivaByUsuarioId(userId);
    }
    
    // Métodos privados auxiliares
    
    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) 
            RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }
    
    private Optional<Long> getEmpresaIdFromSesionCaja() {
        return getCurrentSesionCaja()
            .map(session -> session.getTerminal().getSucursal().getEmpresa().getId());
    }
    
    private Optional<Long> getSucursalIdFromSesionCaja() {
        return getCurrentSesionCaja()
            .map(session -> session.getTerminal().getSucursal().getId());
    }
    
    private Optional<Long> getTerminalIdFromSesionCaja() {
        return getCurrentSesionCaja()
            .map(session -> session.getTerminal().getId());
    }
}