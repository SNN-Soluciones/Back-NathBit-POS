package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Usuario;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    Optional<Usuario> findByEmail(String email);
    
    boolean existsByEmail(String email);

    Optional<Usuario> findByUsernameIgnoreCase(String username);

    // Buscar por empresa
    @Query("SELECT DISTINCT u FROM Usuario u " +
        "JOIN u.usuarioEmpresas ue " +
        "WHERE ue.empresa.id = :empresaId AND ue.activo = true")
    Page<Usuario> findByEmpresaId(@Param("empresaId") Long empresaId, Pageable pageable);

    // Buscar por sucursal
    @Query("SELECT DISTINCT u FROM Usuario u " +
        "JOIN u.usuarioSucursales us " +
        "WHERE us.sucursal.id = :sucursalId AND us.activo = true")
    Page<Usuario> findBySucursalId(@Param("sucursalId") Long sucursalId, Pageable pageable);

    // Buscar por múltiples empresas
    @Query("SELECT DISTINCT u FROM Usuario u " +
        "JOIN u.usuarioEmpresas ue " +
        "WHERE ue.empresa.id IN :empresasIds AND ue.activo = true")
    Page<Usuario> findByEmpresasIds(@Param("empresasIds") List<Long> empresasIds, Pageable pageable);

    // Buscar por múltiples sucursales
    @Query("SELECT DISTINCT u FROM Usuario u " +
        "JOIN u.usuarioSucursales us " +
        "WHERE us.sucursal.id IN :sucursalesIds AND us.activo = true")
    Page<Usuario> findBySucursalesIds(@Param("sucursalesIds") List<Long> sucursalesIds, Pageable pageable);

    // Buscar usuarios con alguna asignación
    @Query("SELECT DISTINCT u FROM Usuario u " +
        "WHERE EXISTS (SELECT 1 FROM UsuarioEmpresa ue WHERE ue.usuario = u AND ue.activo = true)")
    Page<Usuario> findConAsignacion(Pageable pageable);

    // Buscar por sucursal validando empresas permitidas
    @Query("SELECT DISTINCT u FROM Usuario u " +
        "JOIN u.usuarioSucursales us " +
        "JOIN us.sucursal s " +
        "WHERE us.sucursal.id = :sucursalId " +
        "AND s.empresa.id IN :empresasPermitidas " +
        "AND us.activo = true")
    Page<Usuario> findBySucursalIdYEmpresasPermitidas(
        @Param("sucursalId") Long sucursalId,
        @Param("empresasPermitidas") List<Long> empresasPermitidas,
        Pageable pageable
    );

    @Query("SELECT DISTINCT u FROM Usuario u " +
        "JOIN UsuarioEmpresa ue ON ue.usuario = u " +
        "WHERE ue.empresa.id = :empresaId " +
        "AND u.activo = true " +
        "AND ue.activo = true " +
        "ORDER BY u.nombre, u.apellidos")
    List<Usuario> findByEmpresaId(@Param("empresaId") Long empresaId);

}