package com.snnsoluciones.backnathbitpos.security;

import com.snnsoluciones.backnathbitpos.security.ContextoUsuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Servicio para validaciones de seguridad y acceso
 */
@Service("seguridadService")
public class SeguridadService {
    
    /**
     * Obtiene el contexto del usuario actual
     */
    public ContextoUsuario getContextoActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof ContextoUsuario) {
            return (ContextoUsuario) auth.getPrincipal();
        }
        return null;
    }
    
    /**
     * Verifica si el usuario actual tiene acceso a una empresa
     */
    public boolean esEmpresaPropia(Long empresaId) {
        ContextoUsuario contexto = getContextoActual();
        if (contexto == null) return false;
        
        // ROOT y SOPORTE tienen acceso a todo
        if ("ROOT".equals(contexto.getRol()) || "SOPORTE".equals(contexto.getRol())) {
            return true;
        }
        
        // Verificar si la empresa está en el contexto
        return empresaId != null && empresaId.equals(contexto.getEmpresaId());
    }
    
    /**
     * Verifica si el usuario actual tiene acceso a una sucursal
     */
    public boolean esSucursalPropia(Long sucursalId) {
        ContextoUsuario contexto = getContextoActual();
        if (contexto == null) return false;
        
        // ROOT y SOPORTE tienen acceso a todo
        if ("ROOT".equals(contexto.getRol()) || "SOPORTE".equals(contexto.getRol())) {
            return true;
        }
        
        // Verificar si la sucursal está en el contexto
        return sucursalId != null && sucursalId.equals(contexto.getSucursalId());
    }
    
    /**
     * Obtiene el ID del usuario actual
     */
    public Long getUserIdActual() {
        ContextoUsuario contexto = getContextoActual();
        return contexto != null ? contexto.getUserId() : null;
    }
    
    /**
     * Obtiene el ID de la empresa del contexto actual
     */
    public Long getEmpresaIdActual() {
        ContextoUsuario contexto = getContextoActual();
        return contexto != null ? contexto.getEmpresaId() : null;
    }
    
    /**
     * Obtiene el ID de la sucursal del contexto actual
     */
    public Long getSucursalIdActual() {
        ContextoUsuario contexto = getContextoActual();
        return contexto != null ? contexto.getSucursalId() : null;
    }
}