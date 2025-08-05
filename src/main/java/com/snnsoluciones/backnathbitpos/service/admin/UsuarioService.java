package com.snnsoluciones.backnathbitpos.service.admin;

import com.snnsoluciones.backnathbitpos.dto.request.UsuarioCreateRequest;
import com.snnsoluciones.backnathbitpos.dto.request.UsuarioUpdateRequest;
import com.snnsoluciones.backnathbitpos.dto.request.CambioPasswordRequest;
import com.snnsoluciones.backnathbitpos.dto.request.AsignarSucursalesRequest;
import com.snnsoluciones.backnathbitpos.dto.response.AuditEventResponse;
import com.snnsoluciones.backnathbitpos.dto.response.UsuarioResponse;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio para gestión de usuarios en el sistema multi-empresa
 */
public interface UsuarioService {

    // ==================== OPERACIONES CRUD ====================

    /**
     * Crea un nuevo usuario y lo asigna a una empresa
     * @param request datos del nuevo usuario incluyendo empresa y rol
     * @return usuario creado con contexto de empresa
     */
    UsuarioResponse crear(UsuarioCreateRequest request);

    /**
     * Actualiza datos básicos de un usuario
     * @param id ID del usuario
     * @param request datos a actualizar
     * @return usuario actualizado
     */
    UsuarioResponse actualizar(UUID id, UsuarioUpdateRequest request);

    /**
     * Obtiene un usuario por ID con contexto de empresa actual
     * @param id ID del usuario
     * @return usuario con información contextual
     */
    UsuarioResponse obtenerPorId(UUID id);

    /**
     * Obtiene un usuario por ID con contexto de una empresa específica
     * @param id ID del usuario
     * @param empresaId ID de la empresa para contexto
     * @return usuario con información contextual de la empresa
     */
    UsuarioResponse obtenerPorIdConContexto(UUID id, UUID empresaId);

    /**
     * Lista usuarios con filtros y paginación
     * @param rol filtrar por rol (opcional)
     * @param sucursalId filtrar por sucursal (opcional)
     * @param search buscar por nombre o email (opcional)
     * @param incluirInactivos incluir usuarios inactivos
     * @param pageable paginación
     * @return página de usuarios
     */
    Page<UsuarioResponse> listar(String rol, UUID sucursalId, String search,
        boolean incluirInactivos, Pageable pageable);

    /**
     * Desactiva un usuario (soft delete)
     * @param id ID del usuario
     */
    void desactivar(UUID id);

    /**
     * Reactiva un usuario previamente desactivado
     * @param id ID del usuario
     */
    void reactivar(UUID id);

    // ==================== GESTIÓN DE ACCESOS ====================

    /**
     * Cambia el rol de un usuario en la empresa actual
     *
     * @param userId   ID del usuario
     * @param nuevoRol nuevo rol a asignar
     * @return
     */
    UsuarioResponse cambiarRol(UUID userId, String nuevoRol);

    /**
     * Asigna un usuario a empresas adicionales
     * @param userId ID del usuario
     * @param empresasRoles mapa de empresaId -> rol
     */
    void asignarEmpresas(UUID userId, Map<UUID, RolNombre> empresasRoles);

    /**
     * Asigna/actualiza sucursales de un usuario en la empresa actual
     * @param userId ID del usuario
     * @param request sucursales y permisos
     */
    void asignarSucursales(UUID userId, AsignarSucursalesRequest request);

    /**
     * Remueve acceso de un usuario a una empresa
     * @param userId ID del usuario
     * @param empresaId ID de la empresa
     */
    void removerAccesoEmpresa(UUID userId, UUID empresaId);

    /**
     * Remueve acceso de un usuario a una sucursal
     * @param userId ID del usuario
     * @param sucursalId ID de la sucursal
     */
    void removerAccesoSucursal(UUID userId, UUID sucursalId);

    // ==================== SEGURIDAD ====================

    /**
     * Cambia la contraseña de un usuario
     * @param id ID del usuario
     * @param request datos de cambio de contraseña
     */
    void cambiarPassword(UUID id, CambioPasswordRequest request);

    /**
     * Bloquea el acceso de un usuario al sistema
     * @param id ID del usuario
     */
    void bloquearUsuario(UUID id);

    /**
     * Desbloquea el acceso de un usuario al sistema
     * @param id ID del usuario
     */
    void desbloquearUsuario(UUID id);

    /**
     * Resetea el contador de intentos fallidos por email
     * @param email email del usuario
     */
    void resetearIntentosFallidos(String email);

    /**
     * Resetea el contador de intentos fallidos por ID
     * @param userId ID del usuario
     */
    void resetearIntentos(UUID userId);

    /**
     * Registra un login exitoso
     * @param email email del usuario
     */
    void manejarLoginExitoso(String email);

    /**
     * Registra un login fallido e incrementa contador
     * @param email email del usuario
     */
    void manejarLoginFallido(String email);

    /**
     * Fuerza el cambio de contraseña en próximo login
     * @param userId ID del usuario
     */
    void forzarCambioPassword(UUID userId);

    // ==================== AUDITORÍA ====================

    /**
     * Obtiene el historial de login de un usuario
     * @param userId ID del usuario
     * @param page página
     * @param size tamaño de página
     * @return lista de eventos de auditoría
     */
    List<AuditEventResponse> obtenerHistorialLogin(UUID userId, int page, int size);

    /**
     * Obtiene el historial de cambios de un usuario
     * @param userId ID del usuario
     * @param page página
     * @param size tamaño de página
     * @return lista de eventos de auditoría
     */
    List<AuditEventResponse> obtenerHistorialCambios(UUID userId, int page, int size);

    /**
     * Cierra todas las sesiones activas de un usuario
     * @param userId ID del usuario
     */
    void cerrarTodasLasSesiones(UUID userId);

    // ==================== VALIDACIONES ====================

    /**
     * Verifica si existe un email en el sistema
     * @param email email a verificar
     * @return true si existe
     */
    boolean existeEmail(String email);

    /**
     * Verifica si existe un email en una empresa específica
     * @param email email a verificar
     * @param empresaId ID de la empresa
     * @return true si existe
     */
    boolean existeEmailEnEmpresa(String email, UUID empresaId);

    /**
     * Verifica si el usuario actual puede gestionar a otro usuario
     * @param targetUserId ID del usuario objetivo
     * @return true si tiene permisos
     */
    boolean canManageUser(UUID targetUserId);

    /**
     * Verifica si un usuario tiene acceso a una empresa
     * @param userId ID del usuario
     * @param empresaId ID de la empresa
     * @return true si tiene acceso
     */
    boolean tieneAccesoEmpresa(UUID userId, UUID empresaId);

    /**
     * Verifica si un usuario tiene acceso a una sucursal
     * @param userId ID del usuario
     * @param sucursalId ID de la sucursal
     * @return true si tiene acceso
     */
    boolean tieneAccesoSucursal(UUID userId, UUID sucursalId);

    // ==================== OPERACIONES POR LOTE ====================

    /**
     * Crea múltiples usuarios de una vez
     * @param requests lista de usuarios a crear
     * @return lista de usuarios creados
     */
    List<UsuarioResponse> crearEnLote(List<UsuarioCreateRequest> requests);

    /**
     * Desactiva múltiples usuarios
     * @param userIds IDs de usuarios a desactivar
     */
    void desactivarEnLote(Set<UUID> userIds);

    /**
     * Asigna múltiples usuarios a una sucursal
     * @param sucursalId ID de la sucursal
     * @param userIds IDs de usuarios
     * @param permisos permisos a asignar
     */
    void asignarUsuariosASucursal(UUID sucursalId, Set<UUID> userIds,
        Map<String, Boolean> permisos);

    // ==================== REPORTES ====================

    /**
     * Obtiene estadísticas de usuarios por empresa
     * @param empresaId ID de la empresa
     * @return mapa con estadísticas
     */
    Map<String, Object> obtenerEstadisticasEmpresa(UUID empresaId);

    /**
     * Obtiene usuarios por rol en la empresa actual
     * @param rol rol a buscar
     * @return lista de usuarios con ese rol
     */
    List<UsuarioResponse> obtenerUsuariosPorRol(RolNombre rol);

    /**
     * Exporta usuarios a formato Excel
     * @param filtros filtros a aplicar
     * @return bytes del archivo Excel
     */
    byte[] exportarUsuarios(Map<String, Object> filtros);

    /**
     * Asigna cajas a un usuario en la sucursal actual
     * @param userId ID del usuario
     * @param cajaIds IDs de las cajas a asignar
     */
    UsuarioResponse asignarCajas(UUID userId, List<UUID> cajaIds);
}