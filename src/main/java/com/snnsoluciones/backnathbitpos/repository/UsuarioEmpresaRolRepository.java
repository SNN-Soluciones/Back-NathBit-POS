package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresaRol;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioEmpresaRolRepository extends JpaRepository<UsuarioEmpresaRol, Long> {

    // Búsquedas básicas
    List<UsuarioEmpresaRol> findByUsuarioId(Long usuarioId);

    List<UsuarioEmpresaRol> findByUsuarioIdAndActivoTrue(Long usuarioId);

    List<UsuarioEmpresaRol> findByEmpresaId(Long empresaId);

    List<UsuarioEmpresaRol> findBySucursalId(Long sucursalId);

    // Búsqueda específica de rol
    Optional<UsuarioEmpresaRol> findByUsuarioIdAndEmpresaIdAndSucursalId(
        Long usuarioId, Long empresaId, Long sucursalId);

    Optional<UsuarioEmpresaRol> findByUsuarioIdAndEmpresaIdAndSucursalIsNull(
        Long usuarioId, Long empresaId);

    // Búsquedas con fetch
    @Query("SELECT uer FROM UsuarioEmpresaRol uer " +
        "JOIN FETCH uer.usuario " +
        "JOIN FETCH uer.empresa " +
        "LEFT JOIN FETCH uer.sucursal " +
        "WHERE uer.usuario.id = :usuarioId " +
        "AND uer.activo = true")
    List<UsuarioEmpresaRol> findByUsuarioIdWithRelaciones(@Param("usuarioId") Long usuarioId);

    // Verificaciones
    boolean existsByUsuarioIdAndEmpresaIdAndSucursalIdAndActivoTrue(
        Long usuarioId, Long empresaId, Long sucursalId);

    boolean existsByUsuarioIdAndEmpresaIdAndSucursalIsNullAndActivoTrue(
        Long usuarioId, Long empresaId);

    // Búsqueda del rol principal - MÉTODO QUE FALTABA
    @Query("SELECT uer FROM UsuarioEmpresaRol uer " +
        "WHERE uer.usuario.id = :usuarioId " +
        "AND uer.activo = true " +
        "AND uer.esPrincipal = true")
    Optional<UsuarioEmpresaRol> findRolPrincipalByUsuarioId(@Param("usuarioId") Long usuarioId);

    // Búsquedas por rol
    @Query("SELECT uer FROM UsuarioEmpresaRol uer " +
        "WHERE uer.empresa.id = :empresaId " +
        "AND uer.rol = :rol " +
        "AND uer.activo = true")
    List<UsuarioEmpresaRol> findByEmpresaIdAndRol(
        @Param("empresaId") Long empresaId,
        @Param("rol") RolNombre rol);

    // Búsquedas con paginación
    @Query("SELECT uer FROM UsuarioEmpresaRol uer " +
        "JOIN FETCH uer.usuario u " +
        "WHERE uer.empresa.id = :empresaId " +
        "AND uer.activo = true " +
        "AND (:rol IS NULL OR uer.rol = :rol) " +
        "ORDER BY u.nombre, u.apellidos")
    Page<UsuarioEmpresaRol> findByEmpresaIdWithUsuarios(
        @Param("empresaId") Long empresaId,
        @Param("rol") RolNombre rol,
        Pageable pageable);

    // Contar usuarios por empresa y rol
    @Query("SELECT COUNT(uer) FROM UsuarioEmpresaRol uer " +
        "WHERE uer.empresa.id = :empresaId " +
        "AND uer.rol = :rol " +
        "AND uer.activo = true")
    Long countByEmpresaIdAndRol(
        @Param("empresaId") Long empresaId,
        @Param("rol") RolNombre rol);

    // Actualizar rol principal
    @Modifying
    @Query("UPDATE UsuarioEmpresaRol uer " +
        "SET uer.esPrincipal = false " +
        "WHERE uer.usuario.id = :usuarioId " +
        "AND uer.id != :exceptoId")
    void desmarcarOtrosComoPrincipal(
        @Param("usuarioId") Long usuarioId,
        @Param("exceptoId") Long exceptoId);

    // Verificar si usuario tiene acceso a empresa (sin considerar sucursal)
    @Query("SELECT CASE WHEN COUNT(uer) > 0 THEN true ELSE false END " +
        "FROM UsuarioEmpresaRol uer " +
        "WHERE uer.usuario.id = :usuarioId " +
        "AND uer.empresa.id = :empresaId " +
        "AND uer.activo = true")
    boolean existsByUsuarioIdAndEmpresaIdAndActivoTrue(
        @Param("usuarioId") Long usuarioId,
        @Param("empresaId") Long empresaId);

    // Verificar si usuario tiene acceso a empresa y sucursal específica
    @Query("SELECT CASE WHEN COUNT(uer) > 0 THEN true ELSE false END " +
        "FROM UsuarioEmpresaRol uer " +
        "WHERE uer.usuario.id = :usuarioId " +
        "AND uer.empresa.id = :empresaId " +
        "AND uer.activo = true " +
        "AND (uer.sucursal IS NULL OR uer.sucursal.id = :sucursalId)")
    boolean tieneAccesoAEmpresaSucursal(
        @Param("usuarioId") Long usuarioId,
        @Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId);

    // Roles vencidos
    @Query("SELECT uer FROM UsuarioEmpresaRol uer " +
        "WHERE uer.fechaVencimiento IS NOT NULL " +
        "AND uer.fechaVencimiento < :fecha " +
        "AND uer.activo = true")
    List<UsuarioEmpresaRol> findRolesVencidos(@Param("fecha") LocalDateTime fecha);

    // Desactivar roles vencidos
    @Modifying
    @Query("UPDATE UsuarioEmpresaRol uer " +
        "SET uer.activo = false " +
        "WHERE uer.fechaVencimiento IS NOT NULL " +
        "AND uer.fechaVencimiento < :fecha " +
        "AND uer.activo = true")
    int desactivarRolesVencidos(@Param("fecha") LocalDateTime fecha);

    // Búsqueda con filtros avanzados
    @Query("SELECT uer FROM UsuarioEmpresaRol uer " +
        "JOIN uer.usuario u " +
        "WHERE (:empresaId IS NULL OR uer.empresa.id = :empresaId) " +
        "AND (:sucursalId IS NULL OR uer.sucursal.id = :sucursalId) " +
        "AND (:rol IS NULL OR uer.rol = :rol) " +
        "AND (:activo IS NULL OR uer.activo = :activo) " +
        "AND (:search IS NULL OR " +
        "     LOWER(u.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
        "     LOWER(u.apellidos) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
        "     LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<UsuarioEmpresaRol> buscarConFiltros(
        @Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId,
        @Param("rol") RolNombre rol,
        @Param("activo") Boolean activo,
        @Param("search") String search,
        Pageable pageable);
}