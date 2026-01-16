// ProductoCategoriaMenuItemRepository.java
package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ProductoCategoriaMenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoCategoriaMenuItemRepository extends JpaRepository<ProductoCategoriaMenuItem, Long> {

    /**
     * Obtener todos los items de una categoría de menú ordenados
     */
    List<ProductoCategoriaMenuItem> findByCategoriaMenuIdOrderByOrdenAsc(Long categoriaMenuId);

    /**
     * Obtener items destacados de una categoría
     */
    List<ProductoCategoriaMenuItem> findByCategoriaMenuIdAndDestacadoTrueOrderByOrdenAsc(Long categoriaMenuId);

    /**
     * Verificar si un producto hijo ya está asignado a una categoría
     */
    boolean existsByCategoriaMenuIdAndProductoHijoId(Long categoriaMenuId, Long productoHijoId);

    /**
     * Eliminar todos los items de una categoría
     */
    void deleteByCategoriaMenuId(Long categoriaMenuId);

    /**
     * Obtener item específico de una categoría
     */
    @Query("SELECT c FROM ProductoCategoriaMenuItem c WHERE c.categoriaMenuId = :categoriaMenuId AND c.productoHijoId = :productoHijoId")
    ProductoCategoriaMenuItem findByCategoriaMenuIdAndProductoHijoId(
        @Param("categoriaMenuId") Long categoriaMenuId, 
        @Param("productoHijoId") Long productoHijoId
    );

    /**
     * Contar items en una categoría
     */
    long countByCategoriaMenuId(Long categoriaMenuId);
}
