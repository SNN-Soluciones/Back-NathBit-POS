package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.auth.ContextoDTO;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.repository.UsuarioEmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.security.ContextoFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Servicio para validaciones de seguridad y permisos
 * Usado principalmente en anotaciones @PreAuthorize
 */
@Slf4j
@Service("seguridadService")
@RequiredArgsConstructor
public class SeguridadService {
    
    private final UsuarioRepository usuarioRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    
    /**
     * Verifica si el usuario actual es de tipo sistema (ROOT o SOPORTE)
     */
    public boolean esUsuarioSistema() {
        Usuario usuario = obtenerUsuarioActual();
        return usuario != null && usuario.esRolSistema();
    }
    
    /**
     * Verifica si el usuario tiene acceso a una empresa específica
     */
    public boolean tieneAccesoEmpresa(Long empresaId) {
        if (empresaId == null) return false;
        
        Usuario usuario = obtenerUsuarioActual();
        if (usuario == null) return false;
        
        // ROOT y SOPORTE tienen acceso total
        if (usuario.esRolSistema()) return true;
        
        // Verificar si tiene asignación activa en la empresa
        return usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaIdAndActivoTrue(
            usuario.getId(), empresaId
        );
    }
    
    /**
     * Verifica si el usuario tiene acceso a una sucursal específica
     */
    public boolean tieneAccesoSucursal(Long sucursalId) {
        if (sucursalId == null) return false;
        
        Usuario usuario = obtenerUsuarioActual();
        if (usuario == null) return false;
        
        // ROOT y SOPORTE tienen acceso total
        if (usuario.esRolSistema()) return true;
        
        // Verificar contexto actual
        ContextoDTO contexto = ContextoFilter.getContextoActual();
        if (contexto == null) return false;
        
        // Si tiene acceso a todas las sucursales de la empresa
        if (contexto.tieneAccesoTodasSucursales()) {
            // Verificar que la sucursal pertenezca a la empresa del contexto
            return verificarSucursalPerteneceEmpresa(sucursalId, contexto.getEmpresaId());
        }
        
        // Si no, solo puede acceder a su sucursal específica
        return contexto.getSucursalId() != null && contexto.getSucursalId().equals(sucursalId);
    }
    
    /**
     * Verifica si la empresa en el contexto coincide con la especificada
     */
    public boolean esEmpresaPropia(Long empresaId) {
        if (empresaId == null) return false;
        
        // ROOT y SOPORTE pueden acceder a cualquier empresa
        if (esUsuarioSistema()) return true;
        
        ContextoDTO contexto = ContextoFilter.getContextoActual();
        return contexto != null && empresaId.equals(contexto.getEmpresaId());
    }
    
    /**
     * Verifica si la sucursal en el contexto coincide con la especificada
     */
    public boolean esSucursalPropia(Long sucursalId) {
        if (sucursalId == null) return false;
        
        // ROOT y SOPORTE pueden acceder a cualquier sucursal
        if (esUsuarioSistema()) return true;
        
        ContextoDTO contexto = ContextoFilter.getContextoActual();
        if (contexto == null) return false;
        
        // Si tiene acceso a todas las sucursales
        if (contexto.tieneAccesoTodasSucursales()) {
            return verificarSucursalPerteneceEmpresa(sucursalId, contexto.getEmpresaId());
        }
        
        return sucursalId.equals(contexto.getSucursalId());
    }
    
    /**
     * Verifica si el usuario tiene un rol específico
     */
    public boolean tieneRol(String rol) {
        Usuario usuario = obtenerUsuarioActual();
        return usuario != null && usuario.getRol().name().equals(rol);
    }
    
    /**
     * Verifica si el usuario tiene alguno de los roles especificados
     */
    public boolean tieneAlgunRol(String... roles) {
        Usuario usuario = obtenerUsuarioActual();
        if (usuario == null) return false;
        
        String rolUsuario = usuario.getRol().name();
        for (String rol : roles) {
            if (rolUsuario.equals(rol)) return true;
        }
        return false;
    }
    
    /**
     * Verifica si el usuario tiene un permiso específico en el contexto actual
     */
    public boolean tienePermiso(String modulo, String accion) {
        ContextoDTO contexto = ContextoFilter.getContextoActual();
        if (contexto == null) return false;
        
        return contexto.tienePermiso(modulo, accion);
    }
    
    /**
     * Verifica si el usuario puede gestionar otros usuarios
     */
    public boolean puedeGestionarUsuarios() {
        Usuario usuario = obtenerUsuarioActual();
        if (usuario == null) return false;
        
        // ROOT y SOPORTE pueden gestionar todos los usuarios
        if (usuario.esRolSistema()) return true;
        
        // SUPER_ADMIN y ADMIN pueden gestionar usuarios de sus empresas
        if (usuario.getRol() == RolNombre.SUPER_ADMIN || 
            usuario.getRol() == RolNombre.ADMIN) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Verifica si el usuario puede ver reportes
     */
    public boolean puedeVerReportes() {
        Usuario usuario = obtenerUsuarioActual();
        if (usuario == null) return false;
        
        // Roles que pueden ver reportes
        return usuario.esRolSistema() || 
               usuario.getRol() == RolNombre.SUPER_ADMIN ||
               usuario.getRol() == RolNombre.ADMIN ||
               usuario.getRol() == RolNombre.JEFE_CAJAS;
    }
    
    /**
     * Verifica si hay un contexto válido establecido
     */
    public boolean hayContextoValido() {
        Usuario usuario = obtenerUsuarioActual();
        if (usuario == null) return false;
        
        // Usuarios de sistema no requieren contexto
        if (usuario.esRolSistema()) return true;
        
        // Otros usuarios requieren contexto
        ContextoDTO contexto = ContextoFilter.getContextoActual();
        return contexto != null && contexto.estaActivo();
    }
    
    /**
     * Obtiene el usuario actual autenticado
     */
    private Usuario obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        
        String username = auth.getName();
        return usuarioRepository.findByEmailOrUsername(username, username).orElse(null);
    }
    
    /**
     * Verifica si una sucursal pertenece a una empresa
     */
    private boolean verificarSucursalPerteneceEmpresa(Long sucursalId, Long empresaId) {
        // TODO: Implementar consulta para verificar relación
        // return sucursalRepository.existsByIdAndEmpresaId(sucursalId, empresaId);
        return true; // Por ahora retornamos true
    }
}