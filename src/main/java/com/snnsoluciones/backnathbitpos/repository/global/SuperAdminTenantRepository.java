package com.snnsoluciones.backnathbitpos.repository.global;

import com.snnsoluciones.backnathbitpos.entity.global.SuperAdminTenant;
import com.snnsoluciones.backnathbitpos.entity.global.Tenant;
import com.snnsoluciones.backnathbitpos.entity.global.UsuarioGlobal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para entidad SuperAdminTenant (relación N:M)
 */
@Repository
public interface SuperAdminTenantRepository extends JpaRepository<SuperAdminTenant, Long> {

    /**
     * Busca la relación por usuario y tenant
     */
    Optional<SuperAdminTenant> findByUsuarioAndTenant(UsuarioGlobal usuario, Tenant tenant);

    /**
     * Busca la relación por IDs de usuario y tenant
     */
    @Query("SELECT sat FROM SuperAdminTenant sat " +
           "WHERE sat.usuario.id = :usuarioId " +
           "AND sat.tenant.id = :tenantId")
    Optional<SuperAdminTenant> findByUsuarioIdAndTenantId(
        @Param("usuarioId") Long usuarioId, 
        @Param("tenantId") Long tenantId
    );

    /**
     * Verifica si existe relación entre usuario y tenant
     */
    boolean existsByUsuarioAndTenant(UsuarioGlobal usuario, Tenant tenant);

    /**
     * Verifica si existe relación activa entre usuario y tenant
     */
    @Query("SELECT COUNT(sat) > 0 FROM SuperAdminTenant sat " +
           "WHERE sat.usuario.id = :usuarioId " +
           "AND sat.tenant.id = :tenantId " +
           "AND sat.activo = true")
    boolean existsActiveByUsuarioIdAndTenantId(
        @Param("usuarioId") Long usuarioId, 
        @Param("tenantId") Long tenantId
    );

    /**
     * Lista todas las relaciones de un usuario
     */
    List<SuperAdminTenant> findByUsuario(UsuarioGlobal usuario);

    /**
     * Lista relaciones activas de un usuario
     */
    List<SuperAdminTenant> findByUsuarioAndActivoTrue(UsuarioGlobal usuario);

    /**
     * Lista todas las relaciones de un tenant
     */
    List<SuperAdminTenant> findByTenant(Tenant tenant);

    /**
     * Lista relaciones activas de un tenant
     */
    List<SuperAdminTenant> findByTenantAndActivoTrue(Tenant tenant);

    /**
     * Lista relaciones por ID de usuario
     */
    List<SuperAdminTenant> findByUsuarioId(Long usuarioId);

    /**
     * Lista relaciones activas por ID de usuario
     */
    List<SuperAdminTenant> findByUsuarioIdAndActivoTrue(Long usuarioId);

    /**
     * Lista relaciones por ID de tenant
     */
    List<SuperAdminTenant> findByTenantId(Long tenantId);

    /**
     * Lista relaciones activas por ID de tenant
     */
    List<SuperAdminTenant> findByTenantIdAndActivoTrue(Long tenantId);

    /**
     * Lista propietarios de un tenant
     */
    List<SuperAdminTenant> findByTenantIdAndEsPropietarioTrueAndActivoTrue(Long tenantId);

    /**
     * Lista todos los tenants donde el usuario es propietario
     */
    @Query("SELECT sat.tenant FROM SuperAdminTenant sat " +
           "WHERE sat.usuario.id = :usuarioId " +
           "AND sat.esPropietario = true " +
           "AND sat.activo = true")
    List<Tenant> findTenantsPropietario(@Param("usuarioId") Long usuarioId);

    /**
     * Cuenta tenants asignados a un usuario
     */
    long countByUsuarioIdAndActivoTrue(Long usuarioId);

    /**
     * Cuenta usuarios asignados a un tenant
     */
    long countByTenantIdAndActivoTrue(Long tenantId);

    /**
     * Elimina todas las relaciones de un usuario
     */
    void deleteByUsuarioId(Long usuarioId);

    /**
     * Elimina todas las relaciones de un tenant
     */
    void deleteByTenantId(Long tenantId);
}
