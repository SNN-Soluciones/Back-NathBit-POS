package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.UsuarioSucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioSucursalRepository extends JpaRepository<UsuarioSucursal, UsuarioSucursal.UsuarioSucursalId> {

    // Buscar todas las sucursales de un usuario
    @Query("SELECT us.sucursal FROM UsuarioSucursal us WHERE us.usuario.id = :usuarioId AND us.activo = true")
    List<Sucursal> findSucursalesByUsuarioId(@Param("usuarioId") Long usuarioId);

    // Buscar sucursales de un usuario en una empresa específica
    @Query("SELECT us.sucursal FROM UsuarioSucursal us " +
           "WHERE us.usuario.id = :usuarioId " +
           "AND us.sucursal.empresa.id = :empresaId " +
           "AND us.activo = true")
    List<Sucursal> findSucursalesByUsuarioIdAndEmpresaId(
        @Param("usuarioId") Long usuarioId, 
        @Param("empresaId") Long empresaId
    );

    // Verificar si existe la relación
    boolean existsByUsuarioIdAndSucursalId(Long usuarioId, Long sucursalId);

    // Contar sucursales activas de un usuario
    long countByUsuarioIdAndActivoTrue(Long usuarioId);

    // Buscar relaciones por usuario
    List<UsuarioSucursal> findByUsuarioId(Long usuarioId);

    // Eliminar todas las relaciones de un usuario
    void deleteByUsuarioId(Long usuarioId);
}