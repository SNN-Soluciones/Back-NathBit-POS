package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FamiliaProducto;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para gestionar las Familias de Productos
 * Soporta búsquedas por empresa y por sucursal
 */
@Repository
public interface FamiliaProductoRepository extends JpaRepository<FamiliaProducto, Long> {

    /**
     * Buscar familias por empresa y opcionalmente por sucursal
     * Si sucursalId es null o 0, busca solo por empresa (familias globales + todas las de sucursales)
     * Si sucursalId tiene valor, busca familias globales de la empresa + las específicas de esa sucursal
     */
    @Query("SELECT f FROM FamiliaProducto f " +
        "WHERE f.empresa.id = :empresaId " +
        "AND (:sucursalId = 0L OR f.sucursal.id = :sucursalId OR f.sucursal IS NULL) " +
        "ORDER BY f.orden ASC")
    List<FamiliaProducto> findByEmpresaAndSucursal(
        @Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId
    );

    /**
     * Buscar familias activas por empresa y opcionalmente por sucursal
     */
    @Query("SELECT f FROM FamiliaProducto f " +
        "WHERE f.empresa.id = :empresaId " +
        "AND f.activa = true " +
        "AND (:sucursalId = 0L OR f.sucursal.id = :sucursalId OR f.sucursal IS NULL) " +
        "ORDER BY f.orden ASC")
    List<FamiliaProducto> findActivasByEmpresaAndSucursal(
        @Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId
    );

    /**
     * Buscar familias con búsqueda por nombre o código
     */
    @Query("SELECT f FROM FamiliaProducto f " +
        "WHERE f.empresa.id = :empresaId " +
        "AND (:sucursalId = 0L OR f.sucursal.id = :sucursalId OR f.sucursal IS NULL) " +
        "AND (LOWER(f.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
        "OR LOWER(f.codigo) LIKE LOWER(CONCAT('%', :busqueda, '%'))) " +
        "ORDER BY f.orden ASC")
    List<FamiliaProducto> buscarPorEmpresaAndSucursal(
        @Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId,
        @Param("busqueda") String busqueda
    );

    /**
     * Verificar si existe una familia con el mismo código en la empresa/sucursal
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END " +
        "FROM FamiliaProducto f " +
        "WHERE f.codigo = :codigo " +
        "AND f.empresa.id = :empresaId " +
        "AND (:sucursalId = 0L OR f.sucursal.id = :sucursalId OR f.sucursal IS NULL)")
    boolean existsByCodigoAndEmpresaAndSucursal(
        @Param("codigo") String codigo,
        @Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId
    );

    /**
     * Verificar si existe una familia con el mismo código, excluyendo un ID
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END " +
        "FROM FamiliaProducto f " +
        "WHERE f.codigo = :codigo " +
        "AND f.empresa.id = :empresaId " +
        "AND (:sucursalId = 0L OR f.sucursal.id = :sucursalId OR f.sucursal IS NULL) " +
        "AND f.id <> :id")
    boolean existsByCodigoAndEmpresaAndSucursalAndIdNot(
        @Param("codigo") String codigo,
        @Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId,
        @Param("id") Long id
    );

    /**
     * Buscar familia por ID, empresa y opcionalmente sucursal
     */
    @Query("SELECT f FROM FamiliaProducto f " +
        "WHERE f.id = :id " +
        "AND f.empresa.id = :empresaId " +
        "AND (:sucursalId = 0L OR f.sucursal.id = :sucursalId OR f.sucursal IS NULL)")
    Optional<FamiliaProducto> findByIdAndEmpresaAndSucursal(
        @Param("id") Long id,
        @Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId
    );

    List<FamiliaProducto> findBySucursalIdAndUpdatedAtAfter(Long sucursalId, LocalDateTime updatedAt);

    List<FamiliaProducto> findBySucursalId(Long sucursalId);
}