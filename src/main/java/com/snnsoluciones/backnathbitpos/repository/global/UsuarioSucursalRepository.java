package com.snnsoluciones.backnathbitpos.repository.global;

import com.snnsoluciones.backnathbitpos.entity.global.UsuarioSucursal;
import com.snnsoluciones.backnathbitpos.entity.global.UsuarioSucursal.UsuarioSucursalId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para UsuarioSucursal
 */
@Repository
public interface UsuarioSucursalRepository extends
    JpaRepository<UsuarioSucursal, UsuarioSucursalId> {
    
    List<UsuarioSucursal> findByUsuarioId(UUID usuarioId);
    
    List<UsuarioSucursal> findBySucursalId(UUID sucursalId);
    
    List<UsuarioSucursal> findByUsuarioIdAndActivoTrue(UUID usuarioId);
    
    @Query("SELECT us FROM UsuarioSucursal us " +
           "WHERE us.usuarioId = :usuarioId " +
           "AND us.sucursal.id = :sucursalId " +
           "AND us.activo = true")
    Optional<UsuarioSucursal> findByUsuarioIdAndSucursalId(@Param("usuarioId") UUID usuarioId,
                                                           @Param("sucursalId") UUID sucursalId);
    
    @Query("SELECT COUNT(us) FROM UsuarioSucursal us " +
           "WHERE us.sucursal.id = :sucursalId AND us.activo = true")
    long countUsuariosActivosBySucursal(@Param("sucursalId") UUID sucursalId);
}
