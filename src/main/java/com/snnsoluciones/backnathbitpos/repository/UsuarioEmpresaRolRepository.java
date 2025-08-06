package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresaRol;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
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
public interface UsuarioEmpresaRolRepository extends JpaRepository<UsuarioEmpresaRol, Long> {

    // Búsquedas básicas
    List<UsuarioEmpresaRol> findByUsuarioId(Long usuarioId);
    
    List<UsuarioEmpresaRol> findByUsuarioIdAndActivoTrue(Long usuarioId);
    
    List<UsuarioEmpresaRol> findByEmpresaId(Long empresaId);
    
    List<UsuarioEmpresaRol> findBySucursalId(Long sucursalId);

    // Búsqueda específica de rol
    Optional<UsuarioEmpresaRol> findByUsuarioIdAndEmpresaIdAndSucursalId(
        Long usuarioId, Long empresaId, Long sucursalId);

    Optional<UsuarioEmpresaRol> findByUsuarioIdAndEmpresaIdAndSucursalIsNull(
        Long usuarioId, Long empresaId);

    // Búsquedas con fetch
    @Query("SELECT uer FROM UsuarioEmpresaRol uer " +
           "JOIN FETCH uer.usuario " +
           "JOIN FETCH uer.empresa " +
           "LEFT JOIN FETCH uer.sucursal " +
           "WHERE uer.usuario.id = :usuarioId " +
           "AND uer.activo = true")
    List<UsuarioEmpresaRol> findByUsuarioIdWithRelaciones(@Param("usuarioId") Long usuarioId);

    // Verificaciones
    boolean existsByUsuarioIdAndEmpresaIdAndSucursalIdAndActivoTrue(
        Long usuarioId, Long empresaId, Long sucursalId);

    boolean existsByUsuarioIdAndEmpresaIdAndSucursalIsNullAndActivoTrue(
        Long usuarioId, Long empresaId);

    /**
     * Verifica si existe una relación usuario-sucursal activa.
     *
     * @param usuarioId ID del usuario
     * @param sucursalId ID de la sucursal
     * @return true si existe y está activa
     */
    boolean existsByUsuarioIdAndSucursalIdAndActivoTrue(Long usuarioId, Long sucursalId);

    // Búsquedas por rol
    List<UsuarioEmpresaRol> findByRolAndActivoTrue(RolNombre rol);

    @Query("SELECT uer FROM UsuarioEmpresaRol uer " +
           "WHERE uer.empresa.id = :empresaId " +
           "AND uer.rol = :rol " +
           "AND uer.activo = true")
    List<UsuarioEmpresaRol> findByEmpresaIdAndRol(@Param("empresaId") Long empresaId, 
                                                   @Param("rol") RolNombre rol);

    // Roles principales
    @Query("SELECT uer FROM UsuarioEmpresaRol uer " +
           "WHERE uer.usuario.id = :usuarioId " +
           "AND uer.esPrincipal = true " +
           "AND uer.activo = true")
    Optional<UsuarioEmpresaRol> findRolPrincipalByUsuarioId(@Param("usuarioId") Long usuarioId);

    // Actualizar rol principal
    @Modifying
    @Query("UPDATE UsuarioEmpresaRol uer " +
           "SET uer.esPrincipal = false " +
           "WHERE uer.usuario.id = :usuarioId " +
           "AND uer.id != :exceptoId")
    void desmarcarRolesPrincipales(@Param("usuarioId") Long usuarioId, 
                                  @Param("exceptoId") Long exceptoId);

    // Roles por vencer
    @Query("SELECT uer FROM UsuarioEmpresaRol uer " +
           "WHERE uer.fechaVencimiento IS NOT NULL " +
           "AND uer.fechaVencimiento <= :fecha " +
           "AND uer.activo = true")
    List<UsuarioEmpresaRol> findRolesPorVencer(@Param("fecha") LocalDateTime fecha);

    // Búsquedas con permisos específicos
    @Query(value = "SELECT * FROM usuarios_empresas_roles uer " +
                   "WHERE uer.empresa_id = :empresaId " +
                   "AND uer.activo = true " +
                   "AND uer.permisos -> :modulo ->> :accion = 'true'", 
           nativeQuery = true)
    List<UsuarioEmpresaRol> findByPermisoEspecifico(@Param("empresaId") Long empresaId,
                                                    @Param("modulo") String modulo,
                                                    @Param("accion") String accion);

    // Estadísticas
    @Query("SELECT uer.rol, COUNT(uer) FROM UsuarioEmpresaRol uer " +
           "WHERE uer.empresa.id = :empresaId " +
           "AND uer.activo = true " +
           "GROUP BY uer.rol")
    List<Object[]> countRolesByEmpresaId(@Param("empresaId") Long empresaId);

    // Búsqueda con filtros
    @Query("SELECT uer FROM UsuarioEmpresaRol uer " +
           "JOIN uer.usuario u " +
           "WHERE uer.empresa.id = :empresaId " +
           "AND (:sucursalId IS NULL OR uer.sucursal.id = :sucursalId OR uer.sucursal IS NULL) " +
           "AND (:rol IS NULL OR uer.rol = :rol) " +
           "AND (:activo IS NULL OR uer.activo = :activo) " +
           "AND (:busqueda IS NULL OR " +
           "     LOWER(u.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "     LOWER(u.apellidos) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "     LOWER(u.email) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
    Page<UsuarioEmpresaRol> buscar(@Param("empresaId") Long empresaId,
                                   @Param("sucursalId") Long sucursalId,
                                   @Param("rol") RolNombre rol,
                                   @Param("activo") Boolean activo,
                                   @Param("busqueda") String busqueda,
                                   Pageable pageable);

    // Verificar jerarquía
    @Query("SELECT CASE WHEN COUNT(uer) > 0 THEN true ELSE false END " +
           "FROM UsuarioEmpresaRol uer " +
           "WHERE uer.usuario.id = :usuarioId " +
           "AND uer.empresa.id = :empresaId " +
           "AND uer.rol IN :roles " +
           "AND uer.activo = true")
    boolean usuarioTieneAlgunRol(@Param("usuarioId") Long usuarioId,
                                @Param("empresaId") Long empresaId,
                                @Param("roles") List<RolNombre> roles);

    // Limpiar roles expirados
    @Modifying
    @Query("UPDATE UsuarioEmpresaRol uer " +
           "SET uer.activo = false " +
           "WHERE uer.fechaVencimiento IS NOT NULL " +
           "AND uer.fechaVencimiento < CURRENT_TIMESTAMP " +
           "AND uer.activo = true")
    int desactivarRolesExpirados();

    // Historial de asignaciones
    @Query("SELECT uer FROM UsuarioEmpresaRol uer " +
           "WHERE uer.asignadoPor = :asignadoPorId " +
           "ORDER BY uer.fechaAsignacion DESC")
    List<UsuarioEmpresaRol> findAsignacionesPorUsuario(@Param("asignadoPorId") Long asignadoPorId);

    /**
     * Cuenta usuarios activos por empresa.
     *
     * @param empresaId ID de la empresa
     * @return cantidad de usuarios activos
     */
    long countByEmpresaIdAndActivoTrue(Long empresaId);

    /**
     * Cuenta usuarios activos por sucursal.
     *
     * @param sucursalId ID de la sucursal
     * @return cantidad de usuarios activos
     */
    long countBySucursalIdAndActivoTrue(Long sucursalId);

    /**
     * Verifica si existe una relación usuario-empresa activa.
     *
     * @param usuarioId ID del usuario
     * @param empresaId ID de la empresa
     * @return true si existe y está activa
     */
    boolean existsByUsuarioIdAndEmpresaIdAndActivoTrue(Long usuarioId, Long empresaId);

    /**
     * Busca usuarios por empresa y usuario en una empresa específica.
     *
     * @param usuarioId ID del usuario
     * @param empresaId ID de la empresa
     * @return Lista de relaciones encontradas
     */
    @Query("SELECT uer FROM UsuarioEmpresaRol uer " +
        "WHERE uer.usuario.id = :usuarioId " +
        "AND uer.empresa.id = :empresaId " +
        "AND uer.activo = true")
    List<UsuarioEmpresaRol> findByUsuarioIdAndEmpresaId(@Param("usuarioId") Long usuarioId,
        @Param("empresaId") Long empresaId);

}