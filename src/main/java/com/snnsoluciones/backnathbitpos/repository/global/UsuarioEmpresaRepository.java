package com.snnsoluciones.backnathbitpos.repository.global;

import com.snnsoluciones.backnathbitpos.entity.global.UsuarioEmpresa;
import com.snnsoluciones.backnathbitpos.entity.global.UsuarioEmpresa.UsuarioEmpresaId;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para UsuarioEmpresa
 */
@Repository
public interface UsuarioEmpresaRepository extends JpaRepository<UsuarioEmpresa, UsuarioEmpresaId> {

    /**
     * Busca todas las empresas de un usuario
     */
    List<UsuarioEmpresa> findByUsuarioId(UUID usuarioId);

    /**
     * Busca todos los usuarios de una empresa
     */
    List<UsuarioEmpresa> findByEmpresaId(UUID empresaId);

    /**
     * Busca empresas activas de un usuario
     */
    List<UsuarioEmpresa> findByUsuarioIdAndActivoTrue(UUID usuarioId);

    /**
     * Busca una relación específica usuario-empresa
     */
    Optional<UsuarioEmpresa> findByUsuarioIdAndEmpresaId(UUID usuarioId, UUID empresaId);

    /**
     * Busca con relaciones cargadas
     */
    @Query("SELECT ue FROM UsuarioEmpresa ue " +
        "LEFT JOIN FETCH ue.empresa " +
        "LEFT JOIN FETCH ue.usuarioSucursales " +
        "WHERE ue.usuario.id = :usuarioId AND ue.activo = true")
    List<UsuarioEmpresa> findActivosByUsuarioIdWithRelations(@Param("usuarioId") UUID usuarioId);

    /**
     * Cuenta usuarios activos por empresa
     */
    @Query("SELECT COUNT(ue) FROM UsuarioEmpresa ue WHERE ue.empresa.id = :empresaId AND ue.activo = true")
    long countUsuariosActivosByEmpresa(@Param("empresaId") UUID empresaId);

    /**
     * Cuenta usuarios por empresa y rol
     */
    @Query("SELECT COUNT(ue) FROM UsuarioEmpresa ue " +
        "WHERE ue.empresa.id = :empresaId " +
        "AND ue.rol = :rol " +
        "AND ue.activo = true")
    long countByEmpresaIdAndRol(@Param("empresaId") UUID empresaId, @Param("rol") RolNombre rol);

    /**
     * Cuenta usuarios activos por empresa
     */
    @Query("SELECT COUNT(ue) FROM UsuarioEmpresa ue " +
        "WHERE ue.empresa.id = :empresaId " +
        "AND ue.activo = true")
    long countByEmpresaIdAndActivoTrue(@Param("empresaId") UUID empresaId);

    /**
     * Busca usuarios por empresa y rol
     */
    @Query("SELECT ue FROM UsuarioEmpresa ue " +
        "JOIN FETCH ue.usuario " +
        "WHERE ue.empresa.id = :empresaId " +
        "AND ue.rol = :rol " +
        "AND ue.activo = true")
    List<UsuarioEmpresa> findByEmpresaIdAndRolAndActivoTrue(
        @Param("empresaId") UUID empresaId,
        @Param("rol") RolNombre rol
    );

    /**
     * Busca propietarios de una empresa
     */
    @Query("SELECT ue FROM UsuarioEmpresa ue " +
        "WHERE ue.empresa.id = :empresaId " +
        "AND ue.esPropietario = true " +
        "AND ue.activo = true")
    List<UsuarioEmpresa> findPropietariosByEmpresa(@Param("empresaId") UUID empresaId);

    /**
     * Verifica si un usuario es propietario de alguna empresa
     */
    @Query("SELECT CASE WHEN COUNT(ue) > 0 THEN true ELSE false END " +
        "FROM UsuarioEmpresa ue " +
        "WHERE ue.usuario.id = :usuarioId " +
        "AND ue.esPropietario = true " +
        "AND ue.activo = true")
    boolean esPropietarioDeAlgunaEmpresa(@Param("usuarioId") UUID usuarioId);

    /**
     * Busca usuarios con acceso a múltiples empresas
     */
    @Query("SELECT ue.usuario.id, COUNT(DISTINCT ue.empresa.id) " +
        "FROM UsuarioEmpresa ue " +
        "WHERE ue.activo = true " +
        "GROUP BY ue.usuario.id " +
        "HAVING COUNT(DISTINCT ue.empresa.id) > 1")
    List<Object[]> findUsuariosMultiEmpresa();

    /**
     * Busca usuarios por empresa con paginación y búsqueda
     */
    @Query("SELECT ue FROM UsuarioEmpresa ue " +
        "JOIN FETCH ue.usuario u " +
        "WHERE ue.empresa.id = :empresaId " +
        "AND ue.activo = :activo " +
        "AND (LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
        "     OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :search, '%')) " +
        "     OR LOWER(u.apellidos) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<UsuarioEmpresa> searchByEmpresa(
        @Param("empresaId") UUID empresaId,
        @Param("search") String search,
        @Param("activo") boolean activo
    );

    /**
     * Busca usuarios que expiran en los próximos días
     */
    @Query("SELECT ue FROM UsuarioEmpresa ue " +
        "WHERE ue.fechaExpiracion IS NOT NULL " +
        "AND ue.fechaExpiracion <= CURRENT_TIMESTAMP + :dias * INTERVAL ('1 day') " +
        "AND ue.activo = true")
    List<UsuarioEmpresa> findProximosAExpirar(@Param("dias") int dias);

    /**
     * Busca usuarios por empresa y sucursal específica
     */
    @Query("SELECT DISTINCT ue FROM UsuarioEmpresa ue " +
        "JOIN ue.usuarioSucursales us " +
        "WHERE ue.empresa.id = :empresaId " +
        "AND us.sucursal.id = :sucursalId " +
        "AND ue.activo = true " +
        "AND us.activo = true")
    List<UsuarioEmpresa> findByEmpresaIdAndSucursalId(
        @Param("empresaId") UUID empresaId,
        @Param("sucursalId") UUID sucursalId
    );

    /**
     * Estadísticas de usuarios por rol en una empresa
     */
    @Query("SELECT ue.rol, COUNT(ue) " +
        "FROM UsuarioEmpresa ue " +
        "WHERE ue.empresa.id = :empresaId " +
        "AND ue.activo = true " +
        "GROUP BY ue.rol")
    List<Object[]> getEstadisticasPorRol(@Param("empresaId") UUID empresaId);

    /**
     * Busca el último usuario asignado a una empresa
     */
    @Query("SELECT ue FROM UsuarioEmpresa ue " +
        "WHERE ue.empresa.id = :empresaId " +
        "ORDER BY ue.fechaAsignacion DESC")
    Optional<UsuarioEmpresa> findUltimoUsuarioAsignado(@Param("empresaId") UUID empresaId);
}