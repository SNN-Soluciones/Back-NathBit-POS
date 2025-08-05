package com.snnsoluciones.backnathbitpos.repository.global;

import com.snnsoluciones.backnathbitpos.entity.global.UsuarioSucursal;
import com.snnsoluciones.backnathbitpos.entity.global.UsuarioSucursal.UsuarioSucursalId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para UsuarioSucursal
 */
@Repository
public interface UsuarioSucursalRepository extends JpaRepository<UsuarioSucursal, UsuarioSucursalId> {

    /**
     * Busca por usuario y sucursal
     */
    @Query("SELECT us FROM UsuarioSucursal us WHERE us.usuarioId = :usuarioId AND us.sucursal.id = :sucursalId")
    Optional<UsuarioSucursal> findByUsuarioIdAndSucursalId(
        @Param("usuarioId") UUID usuarioId,
        @Param("sucursalId") UUID sucursalId
    );

    /**
     * Lista todas las sucursales de un usuario
     */
    List<UsuarioSucursal> findByUsuarioId(UUID usuarioId);

    /**
     * Lista todos los usuarios de una sucursal
     */
    @Query("SELECT us FROM UsuarioSucursal us WHERE us.sucursal.id = :sucursalId AND us.activo = true")
    List<UsuarioSucursal> findActivosBySucursalId(@Param("sucursalId") UUID sucursalId);

    /**
     * Cuenta usuarios activos en una sucursal
     */
    @Query("SELECT COUNT(us) FROM UsuarioSucursal us WHERE us.sucursal.id = :sucursalId AND us.activo = true")
    long countActivosBySucursal(@Param("sucursalId") UUID sucursalId);

    /**
     * Busca la sucursal principal de un usuario en una empresa
     */
    @Query("SELECT us FROM UsuarioSucursal us " +
        "WHERE us.usuarioId = :usuarioId " +
        "AND us.empresaId = :empresaId " +
        "AND us.esPrincipal = true " +
        "AND us.activo = true")
    Optional<UsuarioSucursal> findSucursalPrincipal(
        @Param("usuarioId") UUID usuarioId,
        @Param("empresaId") UUID empresaId
    );
}