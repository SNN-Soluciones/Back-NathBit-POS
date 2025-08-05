package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.enums.TipoEmpresa;
import com.snnsoluciones.backnathbitpos.enums.PlanSuscripcion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    // Búsquedas básicas
    Optional<Empresa> findByCodigo(String codigo);
    
    Optional<Empresa> findByCodigoAndActivaTrue(String codigo);
    
    boolean existsByCodigo(String codigo);
    
    boolean existsByCedulaJuridica(String cedulaJuridica);

    // Búsquedas con relaciones
    @Query("SELECT DISTINCT e FROM Empresa e " +
           "LEFT JOIN FETCH e.sucursales s " +
           "WHERE e.id = :id")
    Optional<Empresa> findByIdWithSucursales(@Param("id") Long id);

    // Empresas por usuario
    @Query("SELECT DISTINCT e FROM Empresa e " +
           "JOIN e.usuarioEmpresaRoles uer " +
           "WHERE uer.usuario.id = :usuarioId " +
           "AND uer.activo = true " +
           "AND e.activa = true " +
           "ORDER BY e.nombre")
    List<Empresa> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    // Empresas donde usuario es admin
    @Query("SELECT DISTINCT e FROM Empresa e " +
           "JOIN e.usuarioEmpresaRoles uer " +
           "WHERE uer.usuario.id = :usuarioId " +
           "AND uer.rol IN ('ROOT', 'SUPER_ADMIN', 'ADMIN') " +
           "AND uer.activo = true " +
           "AND e.activa = true")
    List<Empresa> findEmpresasAdministradasPorUsuario(@Param("usuarioId") Long usuarioId);

    // Búsquedas con filtros
    @Query("SELECT e FROM Empresa e " +
           "WHERE (:busqueda IS NULL OR " +
           "      LOWER(e.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "      LOWER(e.nombreComercial) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "      e.codigo LIKE CONCAT('%', :busqueda, '%') OR " +
           "      e.cedulaJuridica LIKE CONCAT('%', :busqueda, '%')) " +
           "AND (:tipo IS NULL OR e.tipo = :tipo) " +
           "AND (:activa IS NULL OR e.activa = :activa)")
    Page<Empresa> buscar(@Param("busqueda") String busqueda, 
                        @Param("tipo") TipoEmpresa tipo, 
                        @Param("activa") Boolean activa, 
                        Pageable pageable);

    // Empresas por tipo
    List<Empresa> findByTipoAndActivaTrue(TipoEmpresa tipo);

    // Empresas por plan
    List<Empresa> findByPlanSuscripcionAndActivaTrue(PlanSuscripcion plan);

    // Estadísticas
    @Query("SELECT COUNT(e) FROM Empresa e WHERE e.activa = true")
    long countEmpresasActivas();

    @Query("SELECT e.tipo, COUNT(e) FROM Empresa e " +
           "WHERE e.activa = true " +
           "GROUP BY e.tipo")
    List<Object[]> countEmpresasPorTipo();

    // Empresas con múltiples sucursales
    @Query("SELECT e FROM Empresa e " +
           "JOIN e.sucursales s " +
           "WHERE e.activa = true " +
           "GROUP BY e " +
           "HAVING COUNT(s) > 1")
    List<Empresa> findEmpresasConMultiplesSucursales();

    // Verificar límites del plan
    @Query("SELECT COUNT(s) FROM Sucursal s " +
           "WHERE s.empresa.id = :empresaId " +
           "AND s.activa = true")
    long countSucursalesActivasByEmpresaId(@Param("empresaId") Long empresaId);

    @Query("SELECT COUNT(DISTINCT uer.usuario) FROM UsuarioEmpresaRol uer " +
           "WHERE uer.empresa.id = :empresaId " +
           "AND uer.activo = true")
    long countUsuariosActivosByEmpresaId(@Param("empresaId") Long empresaId);

    // Verificar permisos
    @Query("SELECT CASE WHEN COUNT(uer) > 0 THEN true ELSE false END " +
           "FROM UsuarioEmpresaRol uer " +
           "WHERE uer.usuario.id = :usuarioId " +
           "AND uer.empresa.id = :empresaId " +
           "AND uer.rol IN ('ROOT', 'SUPER_ADMIN', 'ADMIN') " +
           "AND uer.activo = true")
    boolean usuarioPuedeAdministrarEmpresa(@Param("usuarioId") Long usuarioId, 
                                          @Param("empresaId") Long empresaId);

    // Empresas sin sucursales activas
    @Query("SELECT e FROM Empresa e " +
           "WHERE e.activa = true " +
           "AND NOT EXISTS (SELECT s FROM Sucursal s WHERE s.empresa = e AND s.activa = true)")
    List<Empresa> findEmpresasSinSucursalesActivas();

    // Búsqueda por configuración JSON
    @Query(value = "SELECT * FROM empresas e " +
                   "WHERE e.configuracion ->> :key = :value " +
                   "AND e.activa = true",
           nativeQuery = true)
    List<Empresa> findByConfiguracion(@Param("key") String key, @Param("value") String value);
}