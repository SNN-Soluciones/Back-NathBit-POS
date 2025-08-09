package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Usuario;
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
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Búsquedas básicas
    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByEmailOrUsername(String email, String username);

    Optional<Usuario> findByEmailAndActivoTrue(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByIdentificacion(String identificacion);

    Optional<Usuario> findByIdentificacion(String identificacion);

    // Búsquedas con joins
    @Query("SELECT DISTINCT u FROM Usuario u " +
        "LEFT JOIN FETCH u.usuarioEmpresas ue " +
        "LEFT JOIN FETCH ue.empresa " +
        "LEFT JOIN FETCH ue.sucursal " +
        "WHERE u.email = :email")
    Optional<Usuario> findByEmailWithEmpresas(@Param("email") String email);

    @Query("SELECT DISTINCT u FROM Usuario u " +
        "LEFT JOIN FETCH u.usuarioEmpresas ue " +
        "LEFT JOIN FETCH ue.empresa " +
        "LEFT JOIN FETCH ue.sucursal " +
        "WHERE u.id = :id")
    Optional<Usuario> findByIdWithEmpresas(@Param("id") Long id);

    // Búsquedas por empresa
    @Query("SELECT DISTINCT u FROM Usuario u " +
        "JOIN u.usuarioEmpresas ue " +
        "WHERE ue.empresa.id = :empresaId " +
        "AND ue.activo = true " +
        "AND u.activo = true")
    Page<Usuario> findByEmpresaId(@Param("empresaId") Long empresaId, Pageable pageable);

    // Búsquedas por sucursal
    @Query("SELECT DISTINCT u FROM Usuario u " +
        "JOIN u.usuarioEmpresas ue " +
        "WHERE ue.empresa.id = :empresaId " +
        "AND (ue.sucursal.id = :sucursalId OR ue.sucursal IS NULL) " +
        "AND ue.activo = true " +
        "AND u.activo = true")
    Page<Usuario> findByEmpresaIdAndSucursalId(@Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId,
        Pageable pageable);

    // Búsquedas por rol (ahora el rol está en Usuario)
    @Query("SELECT DISTINCT u FROM Usuario u " +
        "JOIN u.usuarioEmpresas ue " +
        "WHERE ue.empresa.id = :empresaId " +
        "AND u.rol = :rol " +
        "AND ue.activo = true " +
        "AND u.activo = true")
    List<Usuario> findByEmpresaIdAndRol(@Param("empresaId") Long empresaId,
        @Param("rol") RolNombre rol);

    // Búsqueda solo por rol
    Page<Usuario> findByRolAndActivoTrue(RolNombre rol, Pageable pageable);

    // Contar por rol
    long countByRolAndActivoTrue(RolNombre rol);

    // Búsquedas con filtros
    @Query("SELECT DISTINCT u FROM Usuario u " +
        "WHERE (:busqueda IS NULL OR :busqueda = '' OR " +
        "      LOWER(u.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
        "      LOWER(u.apellidos) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
        "      LOWER(u.email) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
        "      u.identificacion LIKE CONCAT('%', :busqueda, '%')) " +
        "AND (:activo IS NULL OR u.activo = :activo) " +
        "AND (:rol IS NULL OR u.rol = :rol)")
    Page<Usuario> buscar(@Param("busqueda") String busqueda,
        @Param("activo") Boolean activo,
        @Param("rol") RolNombre rol,
        Pageable pageable);

    // Usuarios bloqueados
    @Query("SELECT u FROM Usuario u WHERE u.bloqueado = true")
    List<Usuario> findUsuariosBloqueados();

    // Usuarios bloqueados con fecha de desbloqueo vencida
    @Query("SELECT u FROM Usuario u WHERE u.bloqueado = true " +
        "AND u.fechaDesbloqueo < CURRENT_TIMESTAMP")
    List<Usuario> findUsuariosPorDesbloquear();

    // Actualización de último acceso
    @Modifying
    @Query("UPDATE Usuario u SET u.ultimoAcceso = :ultimoAcceso WHERE u.id = :id")
    void actualizarUltimoAcceso(@Param("id") Long id, @Param("ultimoAcceso") LocalDateTime ultimoAcceso);

    // Actualización de intentos fallidos
    @Modifying
    @Query("UPDATE Usuario u SET u.intentosFallidos = :intentos, " +
        "u.bloqueado = :bloqueado, u.fechaUltimoIntento = CURRENT_TIMESTAMP " +
        "WHERE u.id = :id")
    void actualizarIntentosFallidos(@Param("id") Long id,
        @Param("intentos") Integer intentos,
        @Param("bloqueado") Boolean bloqueado);

    // Resetear intentos fallidos
    @Modifying
    @Query("UPDATE Usuario u SET u.intentosFallidos = 0, u.bloqueado = false, " +
        "u.fechaDesbloqueo = null WHERE u.id = :id")
    void resetearIntentosFallidos(@Param("id") Long id);

    // Contar usuarios por empresa
    @Query("SELECT COUNT(DISTINCT u) FROM Usuario u " +
        "JOIN u.usuarioEmpresas ue " +
        "WHERE ue.empresa.id = :empresaId " +
        "AND ue.activo = true " +
        "AND u.activo = true")
    long countUsuariosActivosByEmpresaId(@Param("empresaId") Long empresaId);

    // Verificar si usuario tiene acceso a empresa
    @Query("SELECT CASE WHEN COUNT(ue) > 0 THEN true ELSE false END " +
        "FROM UsuarioEmpresa ue " +
        "WHERE ue.usuario.id = :usuarioId " +
        "AND ue.empresa.id = :empresaId " +
        "AND ue.activo = true")
    boolean tieneAccesoAEmpresa(@Param("usuarioId") Long usuarioId,
        @Param("empresaId") Long empresaId);

    // Usuarios sin actividad reciente
    @Query("SELECT u FROM Usuario u " +
        "WHERE u.ultimoAcceso < :fecha " +
        "OR u.ultimoAcceso IS NULL")
    List<Usuario> findUsuariosSinActividadDesde(@Param("fecha") LocalDateTime fecha);

    /**
     * Busca usuarios por sucursal específica.
     * Incluye usuarios que:
     * 1. Tienen acceso directo a esa sucursal específica
     * 2. Tienen acceso a todas las sucursales de la empresa (sucursal = null)
     */
    @Query("SELECT DISTINCT u FROM Usuario u " +
        "JOIN u.usuarioEmpresas ue " +
        "LEFT JOIN ue.sucursal s " +
        "WHERE ue.activo = true " +
        "AND u.activo = true " +
        "AND (" +
        "  (ue.sucursal.id = :sucursalId) " +  // Acceso directo a la sucursal
        "  OR " +
        "  (ue.sucursal IS NULL " +             // Acceso a todas las sucursales
        "   AND ue.empresa.id = (SELECT suc.empresa.id FROM Sucursal suc WHERE suc.id = :sucursalId))" +
        ")")
    Page<Usuario> findBySucursalId(@Param("sucursalId") Long sucursalId, Pageable pageable);

    /**
     * Busca usuarios con filtros múltiples
     */
    @Query("SELECT DISTINCT u FROM Usuario u " +
        "LEFT JOIN u.usuarioEmpresas ue " +
        "WHERE u.activo = true " +
        "AND (:empresaId IS NULL OR ue.empresa.id = :empresaId) " +
        "AND (:sucursalId IS NULL OR " +
        "     (ue.sucursal.id = :sucursalId OR " +
        "      (ue.sucursal IS NULL AND ue.empresa.id = (SELECT s.empresa.id FROM Sucursal s WHERE s.id = :sucursalId)))) " +
        "AND (:search IS NULL OR :search = '' OR " +
        "     LOWER(u.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
        "     LOWER(u.apellidos) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
        "     LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
        "     u.identificacion LIKE CONCAT('%', :search, '%')) " +
        "AND (:rol IS NULL OR u.rol = :rol)")
    Page<Usuario> buscarUsuarios(@Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId,
        @Param("search") String search,
        @Param("rol") RolNombre rol,
        Pageable pageable);

    /**
     * Busca usuarios del sistema (ROOT y SOPORTE)
     */
    @Query("SELECT u FROM Usuario u WHERE u.rol IN ('ROOT', 'SOPORTE') AND u.activo = true")
    List<Usuario> findUsuariosSistema();

    /**
     * Busca usuarios administrativos de una empresa
     */
    @Query("SELECT DISTINCT u FROM Usuario u " +
        "JOIN u.usuarioEmpresas ue " +
        "WHERE ue.empresa.id = :empresaId " +
        "AND u.rol IN ('SUPER_ADMIN', 'ADMIN') " +
        "AND ue.activo = true " +
        "AND u.activo = true")
    List<Usuario> findUsuariosAdministrativosByEmpresaId(@Param("empresaId") Long empresaId);

    /**
     * Busca usuarios operativos de una sucursal
     */
    @Query("SELECT DISTINCT u FROM Usuario u " +
        "JOIN u.usuarioEmpresas ue " +
        "WHERE (ue.sucursal.id = :sucursalId OR " +
        "       (ue.sucursal IS NULL AND ue.empresa.id = (SELECT s.empresa.id FROM Sucursal s WHERE s.id = :sucursalId))) " +
        "AND u.rol IN ('CAJERO', 'MESERO', 'JEFE_CAJAS', 'COCINERO') " +
        "AND ue.activo = true " +
        "AND u.activo = true")
    List<Usuario> findUsuariosOperativosBySucursalId(@Param("sucursalId") Long sucursalId);

    /**
     * Actualiza el token de recuperación
     */
    @Modifying
    @Query("UPDATE Usuario u SET u.tokenRecuperacion = :token, " +
        "u.fechaTokenRecuperacion = CURRENT_TIMESTAMP WHERE u.email = :email")
    void actualizarTokenRecuperacion(@Param("email") String email, @Param("token") String token);

    /**
     * Busca por token de recuperación válido
     */
    @Query("SELECT u FROM Usuario u WHERE u.tokenRecuperacion = :token " +
        "AND u.fechaTokenRecuperacion > :fechaLimite")
    Optional<Usuario> findByTokenRecuperacionValido(@Param("token") String token,
        @Param("fechaLimite") LocalDateTime fechaLimite);

    /**
     * Cuenta usuarios por tipo
     */
    @Query("SELECT u.rol, COUNT(u) FROM Usuario u " +
        "WHERE u.activo = true " +
        "GROUP BY u.rol")
    List<Object[]> contarUsuariosPorRol();

    /**
     * Verifica si un usuario puede acceder a una sucursal
     */
    @Query("SELECT CASE WHEN COUNT(ue) > 0 THEN true ELSE false END " +
        "FROM UsuarioEmpresa ue " +
        "WHERE ue.usuario.id = :usuarioId " +
        "AND ue.activo = true " +
        "AND (" +
        "  ue.sucursal.id = :sucursalId OR " +
        "  (ue.sucursal IS NULL AND ue.empresa.id = (SELECT s.empresa.id FROM Sucursal s WHERE s.id = :sucursalId))" +
        ")")
    boolean tieneAccesoASucursal(@Param("usuarioId") Long usuarioId,
        @Param("sucursalId") Long sucursalId);
}