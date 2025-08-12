package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.CategoriaProducto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaProductoRepository extends JpaRepository<CategoriaProducto, Long> {

    // Buscar por nombre y empresa
    Optional<CategoriaProducto> findByNombreAndEmpresaId(String nombre, Long empresaId);

    // Verificar si existe
    boolean existsByNombreAndEmpresaId(String nombre, Long empresaId);

    // Todas las categorías activas de una empresa, ordenadas
    List<CategoriaProducto> findByEmpresaIdAndActivoTrueOrderByOrdenAscNombreAsc(Long empresaId);

    // Búsqueda paginada por empresa
    @Query("""
           SELECT c
           FROM CategoriaProducto c
           WHERE c.empresa.id = :empresaId
             AND c.activo = true
             AND (
                 LOWER(c.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                 OR LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%'))
             )
           """)
    Page<CategoriaProducto> buscarPorEmpresa(@Param("empresaId") Long empresaId,
        @Param("busqueda") String busqueda,
        Pageable pageable);

    // Contar productos activos por categoría (usar la colección 'categorias' del Producto)
    @Query("""
           SELECT COUNT(p)
           FROM Producto p
           JOIN p.categorias c
           WHERE c.id = :categoriaId
             AND p.activo = true
           """)
    long contarProductosActivos(@Param("categoriaId") Long categoriaId);

    // Siguiente orden disponible dentro de la empresa
    @Query("""
           SELECT COALESCE(MAX(c.orden), 0) + 1
           FROM CategoriaProducto c
           WHERE c.empresa.id = :empresaId
           """)
    Integer obtenerSiguienteOrden(@Param("empresaId") Long empresaId);
}