package com.snnsoluciones.backnathbitpos.service.usuario;

import com.snnsoluciones.backnathbitpos.dto.usuario.ActualizarUsuarioRequest;
import com.snnsoluciones.backnathbitpos.dto.usuario.AsignarRolRequest;
import com.snnsoluciones.backnathbitpos.dto.usuario.CrearUsuarioRequest;
import com.snnsoluciones.backnathbitpos.dto.usuario.UsuarioDTO;
import com.snnsoluciones.backnathbitpos.dto.usuario.UsuarioEmpresaRolDTO;
import java.util.Map;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Servicio para gestión completa de usuarios, roles y asignaciones.
 * Extiende las operaciones básicas de UsuarioService con funcionalidades de gestión.
 */
public interface UsuarioGestionService {
    
    /**
     * Crea un nuevo usuario con rol inicial asignado.
     * 
     * @param request datos del usuario y rol inicial
     * @return UsuarioDTO creado
     */
    UsuarioDTO crearUsuario(CrearUsuarioRequest request) throws BadRequestException;
    
    /**
     * Actualiza la información de un usuario existente.
     * No actualiza contraseña ni roles.
     * 
     * @param id ID del usuario
     * @param request datos a actualizar
     * @return UsuarioDTO actualizado
     */
    UsuarioDTO actualizarUsuario(Long id, ActualizarUsuarioRequest request);
    
    /**
     * Asigna un nuevo rol a un usuario en una empresa/sucursal.
     * 
     * @param usuarioId ID del usuario
     * @param request datos del rol a asignar
     * @return UsuarioEmpresaRolDTO creado
     */
    UsuarioEmpresaRolDTO asignarRol(Long usuarioId, AsignarRolRequest request)
        throws BadRequestException;
    
    /**
     * Remueve un rol específico de un usuario.
     * 
     * @param usuarioId ID del usuario
     * @param usuarioEmpresaRolId ID del rol a remover
     */
    void removerRol(Long usuarioId, Long usuarioEmpresaRolId) throws BadRequestException;
    
    /**
     * Lista todos los usuarios de una empresa con filtros.
     * 
     * @param empresaId ID de la empresa
     * @param sucursalId ID de la sucursal (opcional)
     * @param rol filtrar por rol (opcional)
     * @param activo filtrar por estado (opcional)
     * @param search búsqueda por nombre/email (opcional)
     * @param pageable configuración de paginación
     * @return Página de usuarios
     */
    Page<UsuarioDTO> listarUsuarios(Long empresaId, Long sucursalId, 
                                   String rol, Boolean activo, 
                                   String search, Pageable pageable);
    
    /**
     * Obtiene todos los roles asignados a un usuario.
     * 
     * @param usuarioId ID del usuario
     * @return Lista de roles asignados
     */
    List<UsuarioEmpresaRolDTO> obtenerRolesUsuario(Long usuarioId);
    
    /**
     * Actualiza los permisos personalizados de un rol asignado.
     * 
     * @param usuarioEmpresaRolId ID del rol asignado
     * @param permisos nuevos permisos
     * @return UsuarioEmpresaRolDTO actualizado
     */
    UsuarioEmpresaRolDTO actualizarPermisosRol(Long usuarioEmpresaRolId, 
                                               Map<String, Map<String, Boolean>> permisos)
        throws BadRequestException;
    
    /**
     * Establece un rol como principal para el usuario.
     * 
     * @param usuarioId ID del usuario
     * @param usuarioEmpresaRolId ID del rol a marcar como principal
     */
    void establecerRolPrincipal(Long usuarioId, Long usuarioEmpresaRolId)
        throws BadRequestException;
    
    /**
     * Transfiere todos los usuarios de una sucursal a otra.
     * Útil para reorganizaciones o cierres de sucursales.
     * 
     * @param sucursalOrigenId ID de la sucursal origen
     * @param sucursalDestinoId ID de la sucursal destino
     * @return cantidad de usuarios transferidos
     */
    int transferirUsuariosSucursal(Long sucursalOrigenId, Long sucursalDestinoId)
        throws BadRequestException;
    
    /**
     * Desactiva todos los usuarios de una empresa o sucursal.
     * 
     * @param empresaId ID de la empresa
     * @param sucursalId ID de la sucursal (opcional)
     * @return cantidad de usuarios desactivados
     */
    int desactivarUsuariosMasivo(Long empresaId, Long sucursalId);
    
    /**
     * Valida si un usuario puede gestionar a otro usuario.
     * Basado en jerarquía de roles y contexto.
     * 
     * @param gestorId ID del usuario que intenta gestionar
     * @param usuarioId ID del usuario a gestionar
     * @param empresaId contexto de empresa
     * @return true si puede gestionarlo
     */
    boolean puedeGestionarUsuario(Long gestorId, Long usuarioId, Long empresaId);
}