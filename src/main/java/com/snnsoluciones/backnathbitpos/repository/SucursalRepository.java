package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SucursalRepository extends JpaRepository<Sucursal, Long> {

    // Búsquedas básicas
    Optional<Sucursal> findByCodigoAndEmpresaId(String codigo, Long empresaId);
    
    boolean existsByCodigoAndEmpresaId(String codigo, Long empresaId);

    // Búsquedas con relaciones
    @Query("SELECT s FROM Sucursal s " +
           "LEFT JOIN FETCH s.empresa " +
           "WHERE s.id = :id")
    Optional<Sucursal> findByIdWithEmpresa(@Param("id") Long id);

    // Sucursales por empresa
    List<Sucursal> findByEmpresaIdAndActivaTrue(Long empresaId);
    
    Page<Sucursal> findByEmpresaId(Long empresaId, Pageable pageable);

    // Sucursales por usuario
    @Query("SELECT DISTINCT s FROM Sucursal s " +
           "JOIN s.usuarioEmpresaRoles uer " +
           "WHERE uer.usuario.id = :usuarioId " +
           "AND uer.activo = true " +
           "AND s.activa = true " +
           "ORDER BY s.empresa.nombre, s.nombre")
    List<Sucursal> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    // Sucursales accesibles por usuario en una empresa
    @Query("SELECT DISTINCT s FROM Sucursal s " +
           "JOIN s.empresa e " +
           "LEFT JOIN s.usuarioEmpresaRoles uer " +
           "WHERE e.id = :empresaId " +
           "AND s.activa = true " +
           "AND (uer.usuario.id = :usuarioId AND uer.activo = true " +
           "     OR EXISTS (SELECT uer2 FROM UsuarioEmpresaRol uer2 " +
           "                WHERE uer2.usuario.id = :usuarioId " +
           "                AND uer2.empresa.id = :empresaId " +
           "                AND uer2.sucursal IS NULL " +
           "                AND uer2.activo = true))")
    List<Sucursal> findSucursalesAccesiblesPorUsuario(@Param("usuarioId") Long usuarioId, 
                                                      @Param("empresaId") Long empresaId);

    // Búsquedas con filtros
    @Query("SELECT s FROM Sucursal s " +
           "WHERE s.empresa.id = :empresaId " +
           "AND (:busqueda IS NULL OR " +
           "     LOWER(s.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "     LOWER(s.codigo) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "     LOWER(s.direccion) LIKE LOWER(CONCAT('%', :busqueda, '%'))) " +
           "AND (:activa IS NULL OR s.activa = :activa)")
    Page<Sucursal> buscarPorEmpresa(@Param("empresaId") Long empresaId,
                                    @Param("busqueda") String busqueda, 
                                    @Param("activa") Boolean activa, 
                                    Pageable pageable);

    // Sucursal principal
    @Query("SELECT s FROM Sucursal s " +
           "WHERE s.empresa.id = :empresaId " +
           "AND s.esPrincipal = true " +
           "AND s.activa = true")
    Optional<Sucursal> findSucursalPrincipalByEmpresaId(@Param("empresaId") Long empresaId);

    // Sucursales por ubicación
    @Query("SELECT s FROM Sucursal s " +
           "WHERE (:provincia IS NULL OR s.provincia = :provincia) " +
           "AND (:canton IS NULL OR s.canton = :canton) " +
           "AND (:distrito IS NULL OR s.distrito = :distrito) " +
           "AND s.activa = true")
    List<Sucursal> findByUbicacion(@Param("provincia") String provincia,
                                   @Param("canton") String canton,
                                   @Param("distrito") String distrito);

    // Estadísticas
    @Query("SELECT COUNT(s) FROM Sucursal s " +
           "WHERE s.empresa.id = :empresaId " +
           "AND s.activa = true")
    long countSucursalesActivasByEmpresaId(@Param("empresaId") Long empresaId);

    @Query("SELECT COUNT(DISTINCT uer.usuario) FROM UsuarioEmpresaRol uer " +
           "WHERE uer.sucursal.id = :sucursalId " +
           "AND uer.activo = true")
    long countUsuariosActivosBySucursalId(@Param("sucursalId") Long sucursalId);

    // Verificar permisos
    @Query("SELECT CASE WHEN COUNT(uer) > 0 THEN true ELSE false END " +
           "FROM UsuarioEmpresaRol uer " +
           "WHERE uer.usuario.id = :usuarioId " +
           "AND uer.empresa.id = :empresaId " +
           "AND (uer.sucursal.id = :sucursalId OR uer.sucursal IS NULL) " +
           "AND uer.activo = true")
    boolean usuarioTieneAccesoASucursal(@Param("usuarioId") Long usuarioId, 
                                       @Param("empresaId") Long empresaId,
                                       @Param("sucursalId") Long sucursalId);

    // Sucursales con capacidad
    @Query("SELECT s FROM Sucursal s " +
           "WHERE s.empresa.id = :empresaId " +
           "AND s.cantidadMesas > 0 " +
           "AND s.activa = true")
    List<Sucursal> findSucursalesConMesas(@Param("empresaId") Long empresaId);

    // Búsqueda por configuración JSON
    @Query(value = "SELECT * FROM sucursales s " +
                   "WHERE s.empresa_id = :empresaId " +
                   "AND s.configuracion ->> :key = :value " +
                   "AND s.activa = true", 
           nativeQuery = true)
    List<Sucursal> findByConfiguracion(@Param("empresaId") Long empresaId,
                                      @Param("key") String key, 
                                      @Param("value") String value);

    // Sucursales abiertas ahora
    @Query(value = "SELECT * FROM sucursales s " +
                   "WHERE s.activa = true " +
                   "AND s.hora_apertura IS NOT NULL " +
                   "AND s.hora_cierre IS NOT NULL " +
                   "AND CURRENT_TIME BETWEEN s.hora_apertura AND s.hora_cierre", 
           nativeQuery = true)
    List<Sucursal> findSucursalesAbiertasAhora();
}