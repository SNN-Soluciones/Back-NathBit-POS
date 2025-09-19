package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Producto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // Buscar por código interno y empresa
    Optional<Producto> findByCodigoInternoAndEmpresaId(String codigoInterno, Long empresaId);

    // Buscar por código de barras
    Optional<Producto> findByCodigoBarrasAndEmpresaId(String codigoBarras, Long empresaId);

    // Verificar duplicados
    boolean existsByCodigoInternoAndEmpresaId(String codigoInterno, Long empresaId);
    boolean existsByNombreAndEmpresaId(String nombre, Long empresaId);
    boolean existsByCodigoBarrasAndEmpresaId(String codigoBarras, Long empresaId);

    // Productos de una empresa (solo activos)
    Page<Producto> findByEmpresaId(Long empresaId, Pageable pageable);
    Page<Producto> findByEmpresaIdAndActivoTrue(Long empresaId, Pageable pageable);
    Page<Producto> findBySucursalId(Long empresaId, Pageable pageable);

    // Búsqueda general
    @Query("""
           SELECT p FROM Producto p
           WHERE p.empresa.id = :empresaId
             AND p.activo = true
             AND (
                 LOWER(p.codigoInterno) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR
                 LOWER(p.codigoBarras)  LIKE LOWER(CONCAT('%', :busqueda, '%')) OR
                 LOWER(p.nombre)        LIKE LOWER(CONCAT('%', :busqueda, '%')) OR
                 LOWER(p.descripcion)   LIKE LOWER(CONCAT('%', :busqueda, '%'))
             )
           """)
    Page<Producto> buscarPorEmpresa(@Param("empresaId") Long empresaId,
        @Param("busqueda") String busqueda,
        Pageable pageable);

    // Por categoría (solo activos)
    @Query("""
           SELECT p
           FROM Producto p
           JOIN p.categorias c
           WHERE c.id = :categoriaId
             AND p.activo = true
           """)
    Page<Producto> findByCategoriaId(@Param("categoriaId") Long categoriaId, Pageable pageable);

    // Productos sin categoría (solo activos) por empresa
    @Query("""
           SELECT p
           FROM Producto p
           WHERE p.empresa.id = :empresaId
             AND p.activo = true
             AND p.categorias IS EMPTY
           """)
    List<Producto> findProductosSinCategoria(@Param("empresaId") Long empresaId);

    // Productos con servicio aplicable
    List<Producto> findByEmpresaIdAndAplicaServicioTrueAndActivoTrue(Long empresaId);

    // Contar productos activos por empresa
    long countByEmpresaIdAndActivoTrue(Long empresaId);

    // Fetch de relaciones necesarias (sin intentar fetch de enums)
    @Query("""
           SELECT DISTINCT p
           FROM Producto p
           LEFT JOIN FETCH p.categorias
           LEFT JOIN FETCH p.empresaCabys ec
           LEFT JOIN FETCH ec.codigoCabys
           WHERE p.id = :id
           """)
    Optional<Producto> findByIdConRelaciones(@Param("id") Long id);

    Optional<Producto> findByNombreAndEmpresaId(String nombre, Long empresaId);

    Optional<Producto> findAllByEmpresaIdAndEmpresaCabys_CodigoCabys_Codigo(Long empresaId, String codigoCabysId);

    /**
     * Buscar productos GLOBALES de una empresa (sin sucursal asignada)
     */
    List<Producto> findByEmpresaIdAndSucursalIdIsNullAndActivoTrue(Long empresaId);

    /**
     * Buscar productos LOCALES de una sucursal específica
     */
    List<Producto> findByEmpresaIdAndSucursalIdAndActivoTrue(Long empresaId, Long sucursalId);

    /**
     * Buscar productos globales con término de búsqueda
     */
    @Query("""
        SELECT p FROM Producto p
        WHERE p.empresa.id = :empresaId
          AND p.sucursal.id IS NULL
          AND p.activo = true
          AND (LOWER(p.codigoInterno) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(p.codigoBarras) LIKE LOWER(CONCAT('%', :termino, '%')))
        """)
    List<Producto> buscarGlobalesPorTermino(@Param("empresaId") Long empresaId,
        @Param("termino") String termino);

    /**
     * Buscar productos locales con término de búsqueda
     */
    @Query("""
        SELECT p FROM Producto p
        WHERE p.empresa.id = :empresaId
          AND p.sucursal.id = :sucursalId
          AND p.activo = true
          AND (LOWER(p.codigoInterno) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(p.codigoBarras) LIKE LOWER(CONCAT('%', :termino, '%')))
        """)
    List<Producto> buscarLocalesPorTermino(@Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId,
        @Param("termino") String termino);

    /**
     * Contar productos activos globales
     */
    long countByEmpresaIdAndSucursalIdIsNullAndActivoTrue(Long empresaId);

    /**
     * Contar productos activos por sucursal
     */
    long countByEmpresaIdAndSucursalIdAndActivoTrue(Long empresaId, Long sucursalId);
}