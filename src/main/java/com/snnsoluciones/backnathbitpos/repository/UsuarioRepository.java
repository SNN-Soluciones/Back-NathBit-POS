package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Búsquedas básicas
    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByEmailAndActivoTrue(String email);
    
    boolean existsByEmail(String email);
    
    boolean existsByIdentificacion(String identificacion);

    Optional<Usuario> findByIdentificacion(String identificacion);

    // Búsquedas con joins
    @Query("SELECT DISTINCT u FROM Usuario u " +
           "LEFT JOIN FETCH u.usuarioEmpresaRoles uer " +
           "LEFT JOIN FETCH uer.empresa " +
           "LEFT JOIN FETCH uer.sucursal " +
           "WHERE u.email = :email")
    Optional<Usuario> findByEmailWithRoles(@Param("email") String email);

    @Query("SELECT DISTINCT u FROM Usuario u " +
           "LEFT JOIN FETCH u.usuarioEmpresaRoles uer " +
           "LEFT JOIN FETCH uer.empresa " +
           "LEFT JOIN FETCH uer.sucursal " +
           "WHERE u.id = :id")
    Optional<Usuario> findByIdWithRoles(@Param("id") Long id);

    // Búsquedas por empresa
    @Query("SELECT DISTINCT u FROM Usuario u " +
           "JOIN u.usuarioEmpresaRoles uer " +
           "WHERE uer.empresa.id = :empresaId " +
           "AND uer.activo = true " +
           "AND u.activo = true")
    Page<Usuario> findByEmpresaId(@Param("empresaId") Long empresaId, Pageable pageable);

    // Búsquedas por sucursal
    @Query("SELECT DISTINCT u FROM Usuario u " +
           "JOIN u.usuarioEmpresaRoles uer " +
           "WHERE uer.empresa.id = :empresaId " +
           "AND (uer.sucursal.id = :sucursalId OR uer.sucursal IS NULL) " +
           "AND uer.activo = true " +
           "AND u.activo = true")
    Page<Usuario> findByEmpresaIdAndSucursalId(@Param("empresaId") Long empresaId, 
                                                @Param("sucursalId") Long sucursalId, 
                                                Pageable pageable);

    // Búsquedas por rol
    @Query("SELECT DISTINCT u FROM Usuario u " +
           "JOIN u.usuarioEmpresaRoles uer " +
           "WHERE uer.empresa.id = :empresaId " +
           "AND uer.rol = :rol " +
           "AND uer.activo = true " +
           "AND u.activo = true")
    List<Usuario> findByEmpresaIdAndRol(@Param("empresaId") Long empresaId, 
                                         @Param("rol") com.snnsoluciones.backnathbitpos.enums.RolNombre rol);

    // Búsquedas con filtros
    @Query("SELECT DISTINCT u FROM Usuario u " +
           "WHERE (:busqueda IS NULL OR " +
           "      LOWER(u.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "      LOWER(u.apellidos) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "      LOWER(u.email) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "      u.identificacion LIKE CONCAT('%', :busqueda, '%')) " +
           "AND (:activo IS NULL OR u.activo = :activo)")
    Page<Usuario> buscar(@Param("busqueda") String busqueda, 
                        @Param("activo") Boolean activo, 
                        Pageable pageable);

    // Usuarios bloqueados
    @Query("SELECT u FROM Usuario u WHERE u.bloqueado = true")
    List<Usuario> findUsuariosBloqueados();

    // Actualización de último acceso
    @Modifying
    @Query("UPDATE Usuario u SET u.ultimoAcceso = :ultimoAcceso WHERE u.id = :id")
    void actualizarUltimoAcceso(@Param("id") Long id, @Param("ultimoAcceso") LocalDateTime ultimoAcceso);

    // Actualización de intentos fallidos
    @Modifying
    @Query("UPDATE Usuario u SET u.intentosFallidos = :intentos, u.bloqueado = :bloqueado WHERE u.id = :id")
    void actualizarIntentosFallidos(@Param("id") Long id, 
                                   @Param("intentos") Integer intentos, 
                                   @Param("bloqueado") Boolean bloqueado);

    // Resetear intentos fallidos
    @Modifying
    @Query("UPDATE Usuario u SET u.intentosFallidos = 0, u.bloqueado = false WHERE u.id = :id")
    void resetearIntentosFallidos(@Param("id") Long id);

    // Contar usuarios por empresa
    @Query("SELECT COUNT(DISTINCT u) FROM Usuario u " +
           "JOIN u.usuarioEmpresaRoles uer " +
           "WHERE uer.empresa.id = :empresaId " +
           "AND uer.activo = true " +
           "AND u.activo = true")
    long countUsuariosActivosByEmpresaId(@Param("empresaId") Long empresaId);

    // Verificar si usuario tiene acceso a empresa
    @Query("SELECT CASE WHEN COUNT(uer) > 0 THEN true ELSE false END " +
           "FROM UsuarioEmpresaRol uer " +
           "WHERE uer.usuario.id = :usuarioId " +
           "AND uer.empresa.id = :empresaId " +
           "AND uer.activo = true")
    boolean tieneAccesoAEmpresa(@Param("usuarioId") Long usuarioId, 
                                @Param("empresaId") Long empresaId);

    // Usuarios sin actividad reciente
    @Query("SELECT u FROM Usuario u " +
           "WHERE u.ultimoAcceso < :fecha " +
           "OR u.ultimoAcceso IS NULL")
    List<Usuario> findUsuariosSinActividadDesde(@Param("fecha") LocalDateTime fecha);

    // Agregar estos métodos al UsuarioRepository.java:

    /**
     * Busca usuarios por sucursal específica.
     * Incluye usuarios que:
     * 1. Tienen acceso directo a esa sucursal específica
     * 2. Tienen acceso a todas las sucursales de la empresa (sucursal = null)
     *
     * @param sucursalId ID de la sucursal
     * @param pageable configuración de paginación
     * @return Página de usuarios con acceso a la sucursal
     */
    @Query("SELECT DISTINCT u FROM Usuario u " +
        "JOIN u.usuarioEmpresaRoles uer " +
        "LEFT JOIN uer.sucursal s " +
        "WHERE uer.activo = true " +
        "AND u.activo = true " +
        "AND (" +
        "  (uer.sucursal.id = :sucursalId) " +  // Acceso directo a la sucursal
        "  OR " +
        "  (uer.sucursal IS NULL " +             // Acceso a todas las sucursales
        "   AND uer.empresa.id = (SELECT suc.empresa.id FROM Sucursal suc WHERE suc.id = :sucursalId))" +
        ")")
    Page<Usuario> findBySucursalId(@Param("sucursalId") Long sucursalId, Pageable pageable);

    /**
     * Busca usuarios con filtro de búsqueda.
     *
     * @param empresaId ID de la empresa (opcional)
     * @param sucursalId ID de la sucursal (opcional)
     * @param search término de búsqueda (opcional)
     * @param pageable configuración de paginación
     * @return Página de usuarios filtrados
     */
    @Query("SELECT DISTINCT u FROM Usuario u " +
        "LEFT JOIN u.usuarioEmpresaRoles uer " +
        "WHERE u.activo = true " +
        "AND (:empresaId IS NULL OR uer.empresa.id = :empresaId) " +
        "AND (:sucursalId IS NULL OR " +
        "     (uer.sucursal.id = :sucursalId OR " +
        "      (uer.sucursal IS NULL AND uer.empresa.id = (SELECT s.empresa.id FROM Sucursal s WHERE s.id = :sucursalId)))) " +
        "AND (:search IS NULL OR :search = '' OR " +
        "     LOWER(u.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
        "     LOWER(u.apellidos) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
        "     LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
        "     u.identificacion LIKE CONCAT('%', :search, '%'))")
    Page<Usuario> buscarUsuarios(@Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId,
        @Param("search") String search,
        Pageable pageable);
}