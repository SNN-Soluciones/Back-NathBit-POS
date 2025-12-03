package com.snnsoluciones.backnathbitpos.repository.global;

import com.snnsoluciones.backnathbitpos.entity.global.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para entidad Tenant
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    /**
     * Busca un tenant por su código único
     */
    Optional<Tenant> findByCodigo(String codigo);

    /**
     * Busca un tenant por su código (case insensitive)
     */
    Optional<Tenant> findByCodigoIgnoreCase(String codigo);

    /**
     * Busca un tenant por nombre de schema
     */
    Optional<Tenant> findBySchemaName(String schemaName);

    /**
     * Busca un tenant por ID de empresa legacy
     */
    Optional<Tenant> findByEmpresaLegacyId(Long empresaLegacyId);

    /**
     * Verifica si existe un tenant con el código dado
     */
    boolean existsByCodigo(String codigo);

    /**
     * Verifica si existe un tenant con el schema dado
     */
    boolean existsBySchemaName(String schemaName);

    /**
     * Lista todos los tenants activos
     */
    List<Tenant> findByActivoTrue();

    /**
     * Lista todos los tenants ordenados por nombre
     */
    List<Tenant> findAllByOrderByNombreAsc();

    /**
     * Lista tenants activos ordenados por nombre
     */
    List<Tenant> findByActivoTrueOrderByNombreAsc();

    /**
     * Busca tenants por coincidencia parcial en nombre o código
     */
    @Query("SELECT t FROM Tenant t WHERE " +
           "LOWER(t.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "LOWER(t.codigo) LIKE LOWER(CONCAT('%', :termino, '%'))")
    List<Tenant> buscarPorTermino(@Param("termino") String termino);

    /**
     * Cuenta tenants activos
     */
    long countByActivoTrue();

    /**
     * Lista tenants asignados a un usuario SUPER_ADMIN
     */
    @Query("SELECT sat.tenant FROM SuperAdminTenant sat " +
           "WHERE sat.usuario.id = :usuarioId " +
           "AND sat.activo = true " +
           "AND sat.tenant.activo = true " +
           "ORDER BY sat.tenant.nombre")
    List<Tenant> findByUsuarioGlobalId(@Param("usuarioId") Long usuarioId);

    /**
     * Lista tenants donde el usuario es propietario
     */
    @Query("SELECT sat.tenant FROM SuperAdminTenant sat " +
           "WHERE sat.usuario.id = :usuarioId " +
           "AND sat.esPropietario = true " +
           "AND sat.activo = true " +
           "AND sat.tenant.activo = true")
    List<Tenant> findByUsuarioPropietarioId(@Param("usuarioId") Long usuarioId);
}
