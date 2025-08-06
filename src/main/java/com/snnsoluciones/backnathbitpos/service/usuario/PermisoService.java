package com.snnsoluciones.backnathbitpos.service.usuario;

import com.snnsoluciones.backnathbitpos.dto.usuario.PermisoDTO;
import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresaRol;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;

import java.util.Map;
import org.apache.coyote.BadRequestException;

/**
 * Servicio para gestión de permisos y validación de accesos.
 * Maneja la lógica de permisos basada en roles y permisos granulares JSON.
 */
public interface PermisoService {
    
    /**
     * Verifica si un usuario tiene un permiso específico en el contexto actual.
     * 
     * @param usuarioId ID del usuario
     * @param empresaId ID de la empresa
     * @param sucursalId ID de la sucursal
     * @param modulo módulo del sistema (ej: "productos", "ordenes")
     * @param accion acción a realizar (ej: "crear", "editar", "eliminar")
     * @return true si tiene el permiso
     */
    boolean tienePermiso(Long usuarioId, Long empresaId, Long sucursalId, 
                        String modulo, String accion);
    
    /**
     * Obtiene todos los permisos de un usuario para una empresa/sucursal.
     * 
     * @param usuarioEmpresaRol relación usuario-empresa-rol
     * @return PermisoDTO con estructura de permisos
     */
    PermisoDTO obtenerPermisos(UsuarioEmpresaRol usuarioEmpresaRol);
    
    /**
     * Obtiene los permisos por defecto para un rol específico.
     * 
     * @param rol nombre del rol
     * @return Mapa de permisos por defecto
     */
    Map<String, Map<String, Boolean>> obtenerPermisosDefaultPorRol(RolNombre rol);
    
    /**
     * Actualiza los permisos personalizados de un usuario.
     * 
     * @param usuarioEmpresaRolId ID de la relación usuario-empresa-rol
     * @param permisos nuevos permisos a aplicar
     * @return PermisoDTO actualizado
     */
    PermisoDTO actualizarPermisos(Long usuarioEmpresaRolId, 
                                  Map<String, Map<String, Boolean>> permisos)
        throws BadRequestException;
    
    /**
     * Verifica si un usuario tiene un rol específico en el contexto.
     * 
     * @param usuarioId ID del usuario
     * @param empresaId ID de la empresa
     * @param sucursalId ID de la sucursal
     * @param roles roles a verificar
     * @return true si tiene alguno de los roles
     */
    boolean tieneRol(Long usuarioId, Long empresaId, Long sucursalId, RolNombre... roles);
    
    /**
     * Valida si el usuario puede realizar una acción basándose en rol y permisos.
     * Combina validación por rol y permisos granulares.
     * 
     * @param usuarioId ID del usuario
     * @param empresaId ID de la empresa
     * @param sucursalId ID de la sucursal
     * @param accion acción completa (ej: "productos.crear")
     * @return true si puede realizar la acción
     */
    boolean puedeRealizarAccion(Long usuarioId, Long empresaId, Long sucursalId, String accion);
    
    /**
     * Obtiene el rol principal de un usuario (si tiene múltiples accesos).
     * 
     * @param usuarioId ID del usuario
     * @return UsuarioEmpresaRol principal o el primero encontrado
     */
    UsuarioEmpresaRol obtenerRolPrincipal(Long usuarioId);
    
    /**
     * Valida si los permisos están bien formados antes de guardarlos.
     * 
     * @param permisos estructura de permisos a validar
     * @return true si la estructura es válida
     */
    boolean validarEstructuraPermisos(Map<String, Map<String, Boolean>> permisos);
}