package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la gestión de relaciones usuario-empresa
 */
@Repository
public interface UsuarioEmpresaRepository extends JpaRepository<UsuarioEmpresa, Long> {
    
    /**
     * Encuentra una asignación específica por usuario, empresa y sucursal
     */
    Optional<UsuarioEmpresa> findByUsuarioIdAndEmpresaIdAndSucursalId(
        Long usuarioId, Long empresaId, Long sucursalId
    );
    
    /**
     * Encuentra asignación a nivel empresa (todas las sucursales)
     */
    Optional<UsuarioEmpresa> findByUsuarioIdAndEmpresaIdAndSucursalIdIsNull(
        Long usuarioId, Long empresaId
    );
    
    /**
     * Encuentra todas las asignaciones activas de un usuario
     */
    @Query("SELECT ue FROM UsuarioEmpresa ue " +
           "WHERE ue.usuario.id = :usuarioId " +
           "AND ue.activo = true " +
           "AND ue.fechaRevocacion IS NULL")
    List<UsuarioEmpresa> findAsignacionesActivasByUsuarioId(@Param("usuarioId") Long usuarioId);
    
    /**
     * Encuentra todas las asignaciones de una empresa
     */
    @Query("SELECT ue FROM UsuarioEmpresa ue " +
           "WHERE ue.empresa.id = :empresaId " +
           "AND ue.activo = true")
    List<UsuarioEmpresa> findByEmpresaIdAndActivoTrue(@Param("empresaId") Long empresaId);
    
    /**
     * Encuentra todas las asignaciones de una sucursal
     */
    @Query("SELECT ue FROM UsuarioEmpresa ue " +
           "WHERE ue.sucursal.id = :sucursalId " +
           "AND ue.activo = true")
    List<UsuarioEmpresa> findBySucursalIdAndActivoTrue(@Param("sucursalId") Long sucursalId);
    
    /**
     * Verifica si existe una asignación activa
     */
    boolean existsByUsuarioIdAndEmpresaIdAndActivoTrue(Long usuarioId, Long empresaId);
    
    /**
     * Cuenta usuarios activos en una empresa
     */
    @Query("SELECT COUNT(DISTINCT ue.usuario) FROM UsuarioEmpresa ue " +
           "WHERE ue.empresa.id = :empresaId " +
           "AND ue.activo = true")
    Long countUsuariosActivosByEmpresaId(@Param("empresaId") Long empresaId);
    
    /**
     * Cuenta usuarios activos en una sucursal
     */
    @Query("SELECT COUNT(DISTINCT ue.usuario) FROM UsuarioEmpresa ue " +
           "WHERE ue.sucursal.id = :sucursalId " +
           "AND ue.activo = true")
    Long countUsuariosActivosBySucursalId(@Param("sucursalId") Long sucursalId);
    
    /**
     * Busca usuarios con un rol específico en una empresa
     */
    @Query("SELECT ue FROM UsuarioEmpresa ue " +
           "JOIN ue.usuario u " +
           "WHERE ue.empresa.id = :empresaId " +
           "AND u.rol = :rol " +
           "AND ue.activo = true")
    List<UsuarioEmpresa> findByEmpresaIdAndRol(
        @Param("empresaId") Long empresaId, 
        @Param("rol") String rol
    );
    
    /**
     * Busca asignaciones con permisos específicos
     */
    @Query(value = "SELECT * FROM usuarios_empresas ue " +
                   "WHERE ue.empresa_id = :empresaId " +
                   "AND ue.activo = true " +
                   "AND ue.permisos @> :permisoJson::jsonb", 
           nativeQuery = true)
    List<UsuarioEmpresa> findByEmpresaIdAndPermisos(
        @Param("empresaId") Long empresaId,
        @Param("permisoJson") String permisoJson
    );
    
    /**
     * Elimina (soft delete) todas las asignaciones de un usuario
     */
    @Query("UPDATE UsuarioEmpresa ue " +
           "SET ue.activo = false, " +
           "ue.fechaRevocacion = CURRENT_TIMESTAMP, " +
           "ue.revocadoPor = :revocadoPor " +
           "WHERE ue.usuario.id = :usuarioId")
    void revocarTodasLasAsignaciones(
        @Param("usuarioId") Long usuarioId,
        @Param("revocadoPor") Long revocadoPor
    );
}