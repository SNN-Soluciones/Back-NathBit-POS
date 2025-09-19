package com.snnsoluciones.backnathbitpos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Deprecated(since = "2.0", forRemoval = true)
@Repository
public interface ProductoCategoriaRepository {

    // Agregar producto a categoría
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO producto_categoria (producto_id, categoria_id) VALUES (:productoId, :categoriaId)",
        nativeQuery = true)
    void agregarProductoACategoria(@Param("productoId") Long productoId, @Param("categoriaId") Long categoriaId);

    // Quitar producto de categoría
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM producto_categoria WHERE producto_id = :productoId AND categoria_id = :categoriaId",
        nativeQuery = true)
    void quitarProductoDeCategoria(@Param("productoId") Long productoId, @Param("categoriaId") Long categoriaId);

    // Quitar producto de todas las categorías
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM producto_categoria WHERE producto_id = :productoId",
        nativeQuery = true)
    void quitarProductoDeTodasLasCategorias(@Param("productoId") Long productoId);

    // Verificar si producto está en categoría
    @Query(value = "SELECT COUNT(*) > 0 FROM producto_categoria WHERE producto_id = :productoId AND categoria_id = :categoriaId",
        nativeQuery = true)
    boolean existeRelacion(@Param("productoId") Long productoId, @Param("categoriaId") Long categoriaId);

    // Contar categorías de un producto
    @Query(value = "SELECT COUNT(*) FROM producto_categoria WHERE producto_id = :productoId",
        nativeQuery = true)
    long contarCategoriasDeProducto(@Param("productoId") Long productoId);

    // Obtener IDs de categorías de un producto
    @Query(value = "SELECT categoria_id FROM producto_categoria WHERE producto_id = :productoId",
        nativeQuery = true)
    List<Long> findCategoriaIdsByProductoId(@Param("productoId") Long productoId);
}