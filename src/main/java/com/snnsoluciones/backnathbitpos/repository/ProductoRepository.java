package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
    
    // Productos de una empresa
    Page<Producto> findByEmpresaIdAndActivoTrue(Long empresaId, Pageable pageable);
    
    // Búsqueda general
    @Query("SELECT p FROM Producto p WHERE p.empresa.id = :empresaId AND p.activo = true AND " +
           "(LOWER(p.codigoInterno) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(p.codigoBarras) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
    Page<Producto> buscarPorEmpresa(@Param("empresaId") Long empresaId, 
                                    @Param("busqueda") String busqueda, 
                                    Pageable pageable);
    
    // Por categoría
    Page<Producto> findByCategoriaIdAndActivoTrue(Long categoriaId, Pageable pageable);
    
    // Productos sin categoría
    @Query("SELECT p FROM Producto p WHERE p.empresa.id = :empresaId AND p.categoria IS NULL AND p.activo = true")
    List<Producto> findProductosSinCategoria(@Param("empresaId") Long empresaId);
    
    // Productos que son servicios
    List<Producto> findByEmpresaIdAndEsServicioTrueAndActivoTrue(Long empresaId);
    
    // Productos con servicio aplicable
    List<Producto> findByEmpresaIdAndAplicaServicioTrueAndActivoTrue(Long empresaId);
    
    // Contar productos activos por empresa
    long countByEmpresaIdAndActivoTrue(Long empresaId);
    
    // Productos con fetch de relaciones para evitar N+1
    @Query("SELECT DISTINCT p FROM Producto p " +
           "LEFT JOIN FETCH p.categoria " +
           "LEFT JOIN FETCH p.empresaCabys ec " +
           "LEFT JOIN FETCH ec.codigoCabys " +
           "LEFT JOIN FETCH p.unidadMedida " +
           "LEFT JOIN FETCH p.moneda " +
           "WHERE p.id = :id")
    Optional<Producto> findByIdConRelaciones(@Param("id") Long id);
}