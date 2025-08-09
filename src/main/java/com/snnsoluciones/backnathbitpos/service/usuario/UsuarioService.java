package com.snnsoluciones.backnathbitpos.service.usuario;

import com.snnsoluciones.backnathbitpos.dto.usuario.AccesoDTO;
import com.snnsoluciones.backnathbitpos.dto.usuario.CrearUsuarioRequest;
import com.snnsoluciones.backnathbitpos.dto.usuario.UsuarioDTO;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Servicio para gestión de usuarios del sistema.
 * Maneja operaciones CRUD y lógica de negocio relacionada con usuarios.
 */
public interface UsuarioService {
    
    /**
     * Busca un usuario por su email.
     * 
     * @param email email del usuario
     * @return Usuario encontrado
     */
    Usuario findByEmail(String email);
    
    /**
     * Busca un usuario por su ID.
     * 
     * @param id ID del usuario
     * @return UsuarioDTO con información del usuario
     */
    UsuarioDTO findById(Long id);
    
    /**
     * Obtiene todos los accesos disponibles para un usuario.
     * Lista todas las empresas/sucursales donde tiene roles asignados.
     * 
     * @param usuarioId ID del usuario
     * @return Lista de accesos con empresa, sucursal, rol y permisos
     */
    List<AccesoDTO> obtenerAccesos(Long usuarioId);
    
    /**
     * Valida si un usuario tiene acceso a una empresa/sucursal específica.
     * 
     * @param usuarioId ID del usuario
     * @param empresaId ID de la empresa
     * @param sucursalId ID de la sucursal (puede ser null para todas)
     * @return true si tiene acceso
     */
    boolean validarAcceso(Long usuarioId, Long empresaId, Long sucursalId);
    
    /**
     * Crea un nuevo usuario en el sistema.
     * 
     * @param request datos del nuevo usuario
     * @return UsuarioDTO del usuario creado
     */
    UsuarioDTO crearUsuario(CrearUsuarioRequest request);
    
    /**
     * Actualiza la información de un usuario existente.
     * 
     * @param id ID del usuario
     * @param usuarioDTO datos actualizados
     * @return UsuarioDTO actualizado
     */
    UsuarioDTO actualizarUsuario(Long id, UsuarioDTO usuarioDTO);
    
    /**
     * Lista usuarios con paginación y filtros.
     * Filtra por empresa/sucursal según el contexto del usuario actual.
     * 
     * @param empresaId ID de la empresa
     * @param sucursalId ID de la sucursal (opcional)
     * @param search término de búsqueda (opcional)
     * @param pageable configuración de paginación
     * @return Página de usuarios
     */
    Page<UsuarioDTO> listarUsuarios(Long empresaId, Long sucursalId, String search, RolNombre rol, Pageable pageable);
    
    /**
     * Activa o desactiva un usuario.
     * 
     * @param id ID del usuario
     * @param activo nuevo estado
     * @return UsuarioDTO actualizado
     */
    UsuarioDTO cambiarEstadoUsuario(Long id, boolean activo);
    
    /**
     * Actualiza el último acceso del usuario.
     * 
     * @param usuarioId ID del usuario
     */
    void actualizarUltimoAcceso(Long usuarioId);
    
    /**
     * Cambia la contraseña de un usuario.
     * 
     * @param usuarioId ID del usuario
     * @param passwordActual contraseña actual
     * @param passwordNueva nueva contraseña
     */
    void cambiarPassword(Long usuarioId, String passwordActual, String passwordNueva);
    
    /**
     * Verifica si existe un usuario con el email dado.
     * 
     * @param email email a verificar
     * @return true si existe
     */
    boolean existePorEmail(String email);
    
    /**
     * Verifica si existe un usuario con la identificación dada.
     * 
     * @param identificacion identificación a verificar
     * @return true si existe
     */
    boolean existePorIdentificacion(String identificacion);
}