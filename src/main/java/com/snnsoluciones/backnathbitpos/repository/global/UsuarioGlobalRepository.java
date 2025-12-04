package com.snnsoluciones.backnathbitpos.repository.global;

import com.snnsoluciones.backnathbitpos.entity.global.UsuarioGlobal;
import com.snnsoluciones.backnathbitpos.entity.global.UsuarioGlobal.RolGlobal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para entidad UsuarioGlobal
 */
@Repository
public interface UsuarioGlobalRepository extends JpaRepository<UsuarioGlobal, Long> {

    /**
     * Busca usuario por email (case insensitive)
     */
    Optional<UsuarioGlobal> findByEmailIgnoreCase(String email);

    /**
     * Busca usuario por email exacto
     */
    Optional<UsuarioGlobal> findByEmail(String email);

    /**
     * Verifica si existe un usuario con el email dado
     */
    boolean existsByEmail(String email);

    /**
     * Verifica si existe un usuario con el email dado (case insensitive)
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Busca usuario por ID de usuario legacy
     */
    Optional<UsuarioGlobal> findByUsuarioLegacyId(Long usuarioLegacyId);

    /**
     * Lista todos los usuarios de un rol específico
     */
    List<UsuarioGlobal> findByRol(RolGlobal rol);

    /**
     * Lista usuarios activos de un rol específico
     */
    List<UsuarioGlobal> findByRolAndActivoTrue(RolGlobal rol);

    /**
     * Lista todos los usuarios activos
     */
    List<UsuarioGlobal> findByActivoTrue();

    /**
     * Lista todos los usuarios ordenados por nombre
     */
    List<UsuarioGlobal> findAllByOrderByNombreAsc();

    /**
     * Lista todos los ROOT y SOPORTE (usuarios de sistema)
     */
    @Query("SELECT u FROM UsuarioGlobal u WHERE u.rol IN ('ROOT', 'SOPORTE') ORDER BY u.nombre")
    List<UsuarioGlobal> findUsuariosSistema();

    /**
     * Lista todos los ROOT y SOPORTE activos
     */
    @Query("SELECT u FROM UsuarioGlobal u WHERE u.rol IN ('ROOT', 'SOPORTE') AND u.activo = true ORDER BY u.nombre")
    List<UsuarioGlobal> findUsuariosSistemaActivos();

    /**
     * Lista todos los SUPER_ADMIN
     */
    List<UsuarioGlobal> findByRolOrderByNombreAsc(RolGlobal rol);

    /**
     * Busca usuarios por coincidencia en nombre o email
     */
    @Query("SELECT u FROM UsuarioGlobal u WHERE " +
           "LOWER(u.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "LOWER(u.apellidos) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :termino, '%'))")
    List<UsuarioGlobal> buscarPorTermino(@Param("termino") String termino);

    /**
     * Lista SUPER_ADMINs asignados a un tenant específico
     */
    @Query("SELECT sat.usuario FROM SuperAdminTenant sat " +
           "WHERE sat.tenant.id = :tenantId " +
           "AND sat.activo = true " +
           "AND sat.usuario.activo = true " +
           "ORDER BY sat.usuario.nombre")
    List<UsuarioGlobal> findByTenantId(@Param("tenantId") Long tenantId);

    /**
     * Lista SUPER_ADMINs propietarios de un tenant
     */
    @Query("SELECT sat.usuario FROM SuperAdminTenant sat " +
           "WHERE sat.tenant.id = :tenantId " +
           "AND sat.esPropietario = true " +
           "AND sat.activo = true " +
           "AND sat.usuario.activo = true")
    List<UsuarioGlobal> findPropietariosByTenantId(@Param("tenantId") Long tenantId);

    /**
     * Cuenta usuarios por rol
     */
    long countByRol(RolGlobal rol);

    /**
     * Cuenta usuarios activos por rol
     */
    long countByRolAndActivoTrue(RolGlobal rol);
}
