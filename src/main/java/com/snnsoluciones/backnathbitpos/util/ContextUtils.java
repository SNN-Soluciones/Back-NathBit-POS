package com.snnsoluciones.backnathbitpos.util;

import com.snnsoluciones.backnathbitpos.config.tenant.TenantContext;
import com.snnsoluciones.backnathbitpos.entity.global.UsuarioGlobal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Utilidad para manejar el contexto de empresa y sucursal
 * en el sistema multi-tenant
 */
public class ContextUtils {
    
    private static final String EMPRESA_ID_ATTRIBUTE = "empresaId";
    private static final String SUCURSAL_ID_ATTRIBUTE = "sucursalId";
    
    /**
     * Obtiene el ID de la empresa actual del contexto
     */
    public static UUID getCurrentEmpresaId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> details = (java.util.Map<String, Object>) auth.getDetails();
            Object empresaId = details.get(EMPRESA_ID_ATTRIBUTE);
            if (empresaId != null) {
                return UUID.fromString(empresaId.toString());
            }
        }
        return null;
    }
    
    /**
     * Obtiene el ID de la sucursal actual del contexto
     */
    public static UUID getCurrentSucursalId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> details = (java.util.Map<String, Object>) auth.getDetails();
            Object sucursalId = details.get(SUCURSAL_ID_ATTRIBUTE);
            if (sucursalId != null) {
                return UUID.fromString(sucursalId.toString());
            }
        }
        return null;
    }
    
    /**
     * Obtiene el tenant actual (schema name)
     */
    public static String getCurrentTenant() {
        return TenantContext.getCurrentTenant();
    }
    
    /**
     * Obtiene el usuario actual
     */
    public static UsuarioGlobal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioGlobal) {
            return (UsuarioGlobal) auth.getPrincipal();
        }
        return null;
    }
    
    /**
     * Verifica si hay un contexto de empresa establecido
     */
    public static boolean hasEmpresaContext() {
        return getCurrentEmpresaId() != null;
    }
    
    /**
     * Verifica si hay un contexto de sucursal establecido
     */
    public static boolean hasSucursalContext() {
        return getCurrentSucursalId() != null;
    }
    
    /**
     * Verifica si el contexto está completo (empresa y sucursal)
     */
    public static boolean hasCompleteContext() {
        return hasEmpresaContext() && hasSucursalContext() && getCurrentTenant() != null;
    }
}