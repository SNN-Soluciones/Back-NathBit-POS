package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FamiliaProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para gestionar las Familias de Productos
 * Provee métodos para CRUD y consultas especializadas
 */
@Repository
public interface FamiliaProductoRepository extends JpaRepository<FamiliaProducto, Long> {

    /**
     * Buscar todas las familias de una empresa ordenadas por orden
     */
    List<FamiliaProducto> findByEmpresaIdOrderByOrdenAsc(Long empresaId);

    /**
     * Buscar familias activas de una empresa
     */
    List<FamiliaProducto> findByEmpresaIdAndActivaTrueOrderByOrdenAsc(Long empresaId);

    /**
     * Verificar si existe una familia con el mismo código en la empresa
     */
    boolean existsByCodigoAndEmpresaId(String codigo, Long empresaId);

    /**
     * Verificar si existe una familia con el mismo código en la empresa, excluyendo un ID
     * Útil para validaciones en actualización
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END " +
        "FROM FamiliaProducto f " +
        "WHERE f.codigo = :codigo " +
        "AND f.empresa.id = :empresaId " +
        "AND f.id <> :id")
    boolean existsByCodigoAndEmpresaIdAndIdNot(
        @Param("codigo") String codigo,
        @Param("empresaId") Long empresaId,
        @Param("id") Long id
    );

    /**
     * Buscar familia por código y empresa
     */
    Optional<FamiliaProducto> findByCodigoAndEmpresaId(String codigo, Long empresaId);

    /**
     * Buscar familia por ID y empresa (seguridad - valida que pertenezca a la empresa)
     */
    Optional<FamiliaProducto> findByIdAndEmpresaId(Long id, Long empresaId);

    /**
     * Buscar familias con búsqueda por nombre o código
     */
    @Query("SELECT f FROM FamiliaProducto f " +
        "WHERE f.empresa.id = :empresaId " +
        "AND (LOWER(f.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
        "OR LOWER(f.codigo) LIKE LOWER(CONCAT('%', :busqueda, '%'))) " +
        "ORDER BY f.orden ASC")
    List<FamiliaProducto> buscarPorEmpresa(
        @Param("empresaId") Long empresaId,
        @Param("busqueda") String busqueda
    );
}