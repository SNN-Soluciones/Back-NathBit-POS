package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import java.time.LocalDateTime;
import java.util.Collection;
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

  // ==================== MÉTODOS EXISTENTES (NO TOCAR) ====================
  
  Page<Producto> findByFamiliaIdAndActivoTrue(Long familiaId, Pageable pageable);
  List<Producto> findByFamiliaIdAndActivoTrue(Long familiaId);
  Optional<Producto> findByCodigoInternoAndEmpresaId(String codigoInterno, Long empresaId);
  Optional<Producto> findByCodigoBarrasAndEmpresaId(String codigoBarras, Long empresaId);
  boolean existsByCodigoInternoAndEmpresaId(String codigoInterno, Long empresaId);
  boolean existsByNombreAndEmpresaId(String nombre, Long empresaId);
  boolean existsByCodigoBarrasAndEmpresaId(String codigoBarras, Long empresaId);
  Page<Producto> findByEmpresaId(Long empresaId, Pageable pageable);
  Page<Producto> findByEmpresaIdAndActivoTrue(Long empresaId, Pageable pageable);
  long countByEmpresaIdAndActivoTrue(Long empresaId);

  /**
   * Encuentra productos que tienen imagen pero no tienen thumbnail
   * (Usado para migración/generación de thumbnails)
   */
  List<Producto> findAllByImagenUrlNotNullAndThumbnailUrlNull();

  /**
   * Busca productos activos por empresa y tipo
   */
  List<Producto> findByEmpresaIdAndTipoAndActivoTrue(Long empresaId, TipoProducto tipo);

  /**
   * Busca productos activos por empresa y tipo (con paginación)
   */
  Page<Producto> findByEmpresaIdAndTipoAndActivoTrue(Long empresaId, TipoProducto tipo, Pageable pageable);

  /**
   * Busca productos por empresa con término de búsqueda
   */
  @Query("""
    SELECT p
    FROM Producto p
    WHERE p.empresa.id = :empresaId
      AND (
        LOWER(p.codigoInterno) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR
        LOWER(p.codigoBarras) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR
        LOWER(p.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR
        LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%'))
      )
    """)
  Page<Producto> buscarPorEmpresa(@Param("empresaId") Long empresaId,
      @Param("busqueda") String busqueda,
      Pageable pageable);
  /**
   * Busca producto por empresa y código interno
   */
  Optional<Producto> findByEmpresaIdAndCodigoInterno(Long empresaId, String codigoInterno);

  /**
   * Busca productos por sucursal
   */
  List<Producto> findBySucursalId(Long sucursalId);

  /**
   * Busca productos por sucursal actualizados después de una fecha
   */
  List<Producto> findBySucursalIdAndUpdatedAtAfter(Long sucursalId, LocalDateTime lastSync);

  /**
   * Obtiene todos los productos con sus relaciones cargadas (para evitar N+1)
   */
  @Query("""
    SELECT DISTINCT p
    FROM Producto p
    LEFT JOIN FETCH p.categorias
    LEFT JOIN FETCH p.impuestos
    LEFT JOIN FETCH p.familia
    """)
  List<Producto> findAllWithRelaciones();

  /**
   * Busca un producto por ID y empresa (validación de pertenencia)
   */
  Optional<Producto> findByIdAndEmpresaId(Long id, Long empresaId);

  /**
   * Verifica si existe un producto con código interno en empresa y sucursal específica
   */
  boolean existsByCodigoInternoAndEmpresaIdAndSucursalId(String codigoInterno, Long empresaId, Long sucursalId);

  @Query("""
      SELECT p
      FROM Producto p
      LEFT JOIN FETCH p.categorias
      LEFT JOIN FETCH p.impuestos
      WHERE p.empresa.id = :empresaId
        AND (p.sucursal.id IS NULL OR p.sucursal.id = :sucursalId)
        AND p.activo = true
        AND (
          LOWER(p.codigoInterno) = LOWER(:busqueda) OR
          LOWER(p.codigoInterno) LIKE LOWER(CONCAT(:busqueda, '%')) OR
          LOWER(p.codigoBarras) = LOWER(:busqueda) OR
          LOWER(p.codigoBarras) LIKE LOWER(CONCAT(:busqueda, '%')) OR
          LOWER(p.nombre) LIKE LOWER(CONCAT(:busqueda, ' %')) OR
          LOWER(p.nombre) LIKE LOWER(CONCAT('% ', :busqueda, ' %')) OR
          LOWER(p.nombre) LIKE LOWER(CONCAT('% ', :busqueda)) OR
          LOWER(p.nombre) = LOWER(:busqueda) OR
          LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%'))
        )
      """)
  Page<Producto> buscarPorSucursal(@Param("sucursalId") Long sucursalId,
      @Param("busqueda") String busqueda,
      Pageable pageable);

  @Query("""
      SELECT p
      FROM Producto p
      JOIN p.categorias c
      WHERE c.id = :categoriaId
        AND p.activo = true
      """)
  Page<Producto> findByCategoriaId(@Param("categoriaId") Long categoriaId, Pageable pageable);

  @Query("""
      SELECT p
      FROM Producto p
      WHERE p.empresa.id = :empresaId
        AND p.activo = true
        AND p.categorias IS EMPTY
      """)
  List<Producto> findProductosSinCategoria(@Param("empresaId") Long empresaId);

  // ==================== NUEVOS MÉTODOS V3 ====================

  // ========== PRODUCTOS GLOBALES (sucursalId = NULL) ==========

  /**
   * Obtiene productos GLOBALES de una empresa (sucursalId = NULL)
   */
  Page<Producto> findByEmpresaIdAndSucursalIdIsNull(Long empresaId, Pageable pageable);

  /**
   * Obtiene productos GLOBALES activos de una empresa
   */
  Page<Producto> findByEmpresaIdAndSucursalIdIsNullAndActivoTrue(Long empresaId, Pageable pageable);

  // ========== PRODUCTOS GLOBALES + LOCALES ==========

  /**
   * Obtiene productos GLOBALES (sucursalId = NULL) + LOCALES de una sucursal específica
   * SQL: WHERE empresa_id = ? AND (sucursal_id IS NULL OR sucursal_id = ?)
   */
  @Query("""
      SELECT p
      FROM Producto p
      WHERE p.empresa.id = :empresaId
        AND (p.sucursal.id IS NULL OR p.sucursal.id = :sucursalId)
      """)
  Page<Producto> findByEmpresaIdAndSucursalIdIsNullOrSucursalId(
      @Param("empresaId") Long empresaId,
      @Param("sucursalId") Long sucursalId,
      Pageable pageable);

  /**
   * Obtiene productos GLOBALES + LOCALES activos
   */
  @Query("""
      SELECT p
      FROM Producto p
      WHERE p.empresa.id = :empresaId
        AND (p.sucursal.id IS NULL OR p.sucursal.id = :sucursalId)
        AND p.activo = true
      """)
  Page<Producto> findByEmpresaIdAndSucursalIdIsNullOrSucursalIdAndActivoTrue(
      @Param("empresaId") Long empresaId,
      @Param("sucursalId") Long sucursalId,
      Pageable pageable);

  // ========== BÚSQUEDAS POR TÉRMINO ==========

  /**
   * Busca productos GLOBALES por término (código interno, código barras, nombre)
   */
  @Query("""
      SELECT p
      FROM Producto p
      WHERE p.empresa.id = :empresaId
        AND p.sucursal.id IS NULL
        AND (
          LOWER(p.codigoInterno) LIKE :termino OR
          LOWER(p.codigoBarras) LIKE :termino OR
          LOWER(p.nombre) LIKE :termino OR
          LOWER(p.descripcion) LIKE :termino
        )
      """)
  Page<Producto> buscarGlobalesPorTermino(
      @Param("empresaId") Long empresaId,
      @Param("termino") String termino,
      Pageable pageable);

  /**
   * Busca productos GLOBALES + LOCALES por término
   */
  @Query("""
      SELECT p
      FROM Producto p
      WHERE p.empresa.id = :empresaId
        AND (p.sucursal.id IS NULL OR p.sucursal.id = :sucursalId)
        AND (
          LOWER(p.codigoInterno) LIKE :termino OR
          LOWER(p.codigoBarras) LIKE :termino OR
          LOWER(p.nombre) LIKE :termino OR
          LOWER(p.descripcion) LIKE :termino
        )
      """)
  Page<Producto> buscarGlobalesYLocalesPorTermino(
      @Param("empresaId") Long empresaId,
      @Param("sucursalId") Long sucursalId,
      @Param("termino") String termino,
      Pageable pageable);

  // ========== BÚSQUEDAS POR CATEGORÍA ==========

  /**
   * Busca productos GLOBALES de una categoría
   */
  @Query("""
      SELECT p
      FROM Producto p
      JOIN p.categorias c
      WHERE c.id = :categoriaId
        AND p.empresa.id = :empresaId
        AND p.sucursal.id IS NULL
              AND p.activo = true
      """)
  Page<Producto> findByCategoriasIdAndEmpresaIdAndSucursalIdIsNull(
      @Param("categoriaId") Long categoriaId,
      @Param("empresaId") Long empresaId,
      Pageable pageable);

  /**
   * Busca productos GLOBALES + LOCALES de una categoría
   */
  @Query("""
      SELECT p
      FROM Producto p
      JOIN p.categorias c
      WHERE c.id = :categoriaId
        AND p.empresa.id = :empresaId
        AND (p.sucursal.id IS NULL OR p.sucursal.id = :sucursalId)
              AND p.activo = true
      """)
  Page<Producto> findByCategoriasIdAndEmpresaIdAndSucursalIdIsNullOrSucursalId(
      @Param("categoriaId") Long categoriaId,
      @Param("empresaId") Long empresaId,
      @Param("sucursalId") Long sucursalId,
      Pageable pageable);

  // ========== BÚSQUEDAS POR FAMILIA ==========

  /**
   * Busca productos GLOBALES de una familia
   */
  @Query("""
      SELECT p
      FROM Producto p
      WHERE p.familia.id = :familiaId
        AND p.empresa.id = :empresaId
        AND p.sucursal.id IS NULL
      """)
  Page<Producto> findByFamiliaIdAndEmpresaIdAndSucursalIdIsNull(
      @Param("familiaId") Long familiaId,
      @Param("empresaId") Long empresaId,
      Pageable pageable);

  /**
   * Busca productos GLOBALES + LOCALES de una familia
   */
  @Query("""
      SELECT p
      FROM Producto p
      WHERE p.familia.id = :familiaId
        AND p.empresa.id = :empresaId
        AND (p.sucursal.id IS NULL OR p.sucursal.id = :sucursalId)
      """)
  Page<Producto> findByFamiliaIdAndEmpresaIdAndSucursalIdIsNullOrSucursalId(
      @Param("familiaId") Long familiaId,
      @Param("empresaId") Long empresaId,
      @Param("sucursalId") Long sucursalId,
      Pageable pageable);

  // ========== UTILIDADES ==========

  /**
   * Obtiene el último código interno de la empresa para generar el siguiente
   * Retorna el código con el número más alto (ej: PROD-00025)
   */
  @Query("""
      SELECT p.codigoInterno
      FROM Producto p
      WHERE p.empresa.id = :empresaId
        AND p.codigoInterno LIKE 'PROD-%'
      ORDER BY p.codigoInterno DESC
      LIMIT 1
      """)
  String findUltimoCodigoInternoByEmpresa(@Param("empresaId") Long empresaId);

  /**
   * Verifica si existe un código de barras (en cualquier empresa)
   */
  boolean existsByCodigoBarras(String codigoBarras);

  /**
   * Verifica si existe un código interno en una empresa específica
   * (ya existe pero lo dejamos documentado)
   */
  // boolean existsByCodigoInternoAndEmpresaId(String codigoInterno, Long empresaId);

  // ========== QUERIES OPTIMIZADAS CON FETCH ==========

  /**
   * Obtiene un producto por ID con todas sus relaciones cargadas (para evitar N+1)
   */
  @Query("""
      SELECT p
      FROM Producto p
      LEFT JOIN FETCH p.categorias
      LEFT JOIN FETCH p.impuestos
      LEFT JOIN FETCH p.familia
      WHERE p.id = :id
      """)
  Optional<Producto> findByIdWithRelations(@Param("id") Long id);

  /**
   * Busca producto por nombre y empresa
   */
  Optional<Producto> findByNombreAndEmpresaId(String nombre, Long empresaId);

  /**
   * Método alias para compatibilidad (mismo que findByIdWithRelations)
   */
  @Query("""
        SELECT p
        FROM Producto p
        LEFT JOIN FETCH p.categorias
        LEFT JOIN FETCH p.impuestos
        LEFT JOIN FETCH p.familia
        WHERE p.id = :id
        """)
  Optional<Producto> findByIdConRelaciones(@Param("id") Long id);

  /**
   * Busca productos con relaciones para listados
   */
  @Query("""
      SELECT DISTINCT p
      FROM Producto p
      LEFT JOIN FETCH p.familia
      WHERE p.empresa.id = :empresaId
        AND (p.sucursal.id IS NULL OR p.sucursal.id = :sucursalId)
      """)
  Page<Producto> findByEmpresaAndSucursalWithFamilia(
      @Param("empresaId") Long empresaId,
      @Param("sucursalId") Long sucursalId,
      Pageable pageable);
}