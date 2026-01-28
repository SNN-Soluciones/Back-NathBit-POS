package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Usuario;
import java.time.LocalDateTime;
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


    @Query("SELECT DISTINCT u FROM Usuario u " +
        "JOIN UsuarioEmpresa ue ON ue.usuario = u " +
        "WHERE ue.empresa.id = :empresaId " +
        "AND u.activo = true " +
        "AND ue.activo = true " +
        "ORDER BY u.nombre, u.apellidos")
    List<Usuario> findByEmpresaId(@Param("empresaId") Long empresaId);

    @Query("SELECT DISTINCT u FROM Usuario u " +
        "LEFT JOIN UsuarioEmpresa ue ON u.id = ue.usuario.id " +
        "WHERE ue.empresa.id = :empresaId " +
        "AND u.rol != 'ROOT'") // Ocultar ROOT
    Page<Usuario> findByEmpresaId(@Param("empresaId") Long empresaId, Pageable pageable);

    @Query("SELECT DISTINCT u FROM Usuario u " +
        "LEFT JOIN UsuarioSucursal us ON u.id = us.usuario.id " +
        "WHERE us.sucursal.id = :sucursalId " +
        "AND u.rol != 'ROOT'")
    Page<Usuario> findBySucursalId(@Param("sucursalId") Long sucursalId, Pageable pageable);

    // Consulta para usuarios de una empresa, filtrando por sucursal pero incluyendo roles superiores
    @Query("SELECT DISTINCT u FROM Usuario u " +
        "LEFT JOIN UsuarioEmpresa ue ON u.id = ue.usuario.id " +
        "LEFT JOIN UsuarioSucursal us ON u.id = us.usuario.id " +
        "WHERE ue.empresa.id = :empresaId " +
        "AND (us.sucursal.id = :sucursalId OR u.rol IN ('SUPER_ADMIN', 'ADMIN')) " +
        "AND u.rol != 'ROOT'")
    Page<Usuario> findByEmpresaIdAndSucursalIdIncludeSuperiores(
        @Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId,
        Pageable pageable);

    // Consulta para SUPER_ADMIN - usuarios de múltiples empresas
    @Query("SELECT DISTINCT u FROM Usuario u " +
        "LEFT JOIN UsuarioEmpresa ue ON u.id = ue.usuario.id " +
        "WHERE ue.empresa.id IN :empresasIds " +
        "AND u.rol != 'ROOT'")
    Page<Usuario> findByEmpresasIds(@Param("empresasIds") List<Long> empresasIds, Pageable pageable);

    // Consulta para ADMIN - usuarios de múltiples sucursales
    @Query("SELECT DISTINCT u FROM Usuario u " +
        "LEFT JOIN UsuarioSucursal us ON u.id = us.usuario.id " +
        "LEFT JOIN UsuarioEmpresa ue ON u.id = ue.usuario.id " +
        "WHERE (us.sucursal.id IN :sucursalesIds " +
        "OR (ue.empresa.id = :empresaId AND u.rol IN ('SUPER_ADMIN', 'ADMIN'))) " +
        "AND u.rol != 'ROOT'")
    Page<Usuario> findBySucursalesIds(
        @Param("sucursalesIds") List<Long> sucursalesIds,
        @Param("empresaId") Long empresaId,
        Pageable pageable);

    // Todos los usuarios excepto ROOT
    @Query("SELECT u FROM Usuario u WHERE u.rol != 'ROOT'")
    Page<Usuario> findAllExceptRoot(Pageable pageable);

    // Usuarios con asignación (empresa o sucursal)
    @Query("SELECT DISTINCT u FROM Usuario u " +
        "LEFT JOIN UsuarioEmpresa ue ON u.id = ue.usuario.id " +
        "LEFT JOIN UsuarioSucursal us ON u.id = us.usuario.id " +
        "WHERE (ue.id IS NOT NULL OR us IS NOT NULL) " +
        "AND u.rol != 'ROOT'")
    Page<Usuario> findConAsignacion(Pageable pageable);

    List<Usuario> findByUsuarioEmpresas_Empresa_IdAndUpdatedAtAfter(Long empresaId, LocalDateTime updatedAt);

    List<Usuario> findByUsuarioEmpresas_Empresa_Id(Long empresaId);

    @Query("""
    SELECT DISTINCT u FROM Usuario u
    JOIN SesionCaja sc ON sc.usuario.id = u.id
    WHERE sc.terminal.sucursal.id = :sucursalId
    AND sc.estado = 'ABIERTA'
    AND sc.fechaHoraCierre IS NULL
    ORDER BY u.nombre, u.apellidos
    """)
    List<Usuario> findCajerosConSesionAbierta(@Param("sucursalId") Long sucursalId);
}