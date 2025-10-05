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

    // Buscar sucursales de un usuario en una empresa específica
    @Query("SELECT us.sucursal FROM UsuarioSucursal us " +
           "WHERE us.usuario.id = :usuarioId " +
           "AND us.sucursal.empresa.id = :empresaId " +
           "AND us.activo = true")
    List<Sucursal> findSucursalesByUsuarioIdAndEmpresaId(
        @Param("usuarioId") Long usuarioId, 
        @Param("empresaId") Long empresaId
    );
    // Buscar relaciones por usuario
    List<UsuarioSucursal> findByUsuarioId(Long usuarioId);
}