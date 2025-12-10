package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.UsuarioSucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para UsuarioSucursal.
 * Métodos adicionales para soporte multi-tenant.
 */
@Repository
public interface UsuarioSucursalRepository extends JpaRepository<UsuarioSucursal, UsuarioSucursal.UsuarioSucursalId> {

    /**
     * Busca asignaciones activas de un usuario
     */
    @Query("SELECT us FROM UsuarioSucursal us " +
           "WHERE us.usuario.id = :usuarioId " +
           "AND us.activo = true")
    List<UsuarioSucursal> findByUsuarioIdAndActivoTrue(@Param("usuarioId") Long usuarioId);

    @Query("SELECT us.sucursal FROM UsuarioSucursal us " +
        "WHERE us.usuario.id = :usuarioId " +
        "AND us.sucursal.empresa.id = :empresaId " +
        "AND us.activo = true")
    List<Sucursal> findSucursalesByUsuarioIdAndEmpresaId(
        @Param("usuarioId") Long usuarioId,
        @Param("empresaId") Long empresaId
    );

    /**
     * Busca asignaciones de un usuario (todas)
     */
    List<UsuarioSucursal> findByUsuarioId(Long usuarioId);

    /**
     * Busca asignaciones activas de una sucursal
     */
    @Query("SELECT us FROM UsuarioSucursal us " +
           "WHERE us.sucursal.id = :sucursalId " +
           "AND us.activo = true")
    List<UsuarioSucursal> findBySucursalIdAndActivoTrue(@Param("sucursalId") Long sucursalId);

    /**
     * Verifica si existe asignación activa
     */
    @Query("SELECT COUNT(us) > 0 FROM UsuarioSucursal us " +
           "WHERE us.usuario.id = :usuarioId " +
           "AND us.sucursal.id = :sucursalId " +
           "AND us.activo = true")
    boolean existsActiveByUsuarioIdAndSucursalId(
        @Param("usuarioId") Long usuarioId, 
        @Param("sucursalId") Long sucursalId
    );

    /**
     * Busca una asignación específica
     */
    @Query("SELECT us FROM UsuarioSucursal us " +
           "WHERE us.usuario.id = :usuarioId " +
           "AND us.sucursal.id = :sucursalId")
    Optional<UsuarioSucursal> findByUsuarioIdAndSucursalId(
        @Param("usuarioId") Long usuarioId, 
        @Param("sucursalId") Long sucursalId
    );

    /**
     * Cuenta sucursales activas de un usuario
     */
    @Query("SELECT COUNT(us) FROM UsuarioSucursal us " +
           "WHERE us.usuario.id = :usuarioId " +
           "AND us.activo = true")
    long countActivasByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * Cuenta usuarios activos en una sucursal
     */
    @Query("SELECT COUNT(us) FROM UsuarioSucursal us " +
           "WHERE us.sucursal.id = :sucursalId " +
           "AND us.activo = true")
    long countActivosBySucursalId(@Param("sucursalId") Long sucursalId);

    /**
     * Elimina todas las asignaciones de un usuario
     */
    void deleteByUsuarioId(Long usuarioId);

    /**
     * Elimina todas las asignaciones de una sucursal
     */
    void deleteBySucursalId(Long sucursalId);
}
