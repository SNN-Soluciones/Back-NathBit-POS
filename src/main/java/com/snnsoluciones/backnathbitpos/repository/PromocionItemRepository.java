package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.PromocionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromocionItemRepository extends JpaRepository<PromocionItem, Long> {

    List<PromocionItem> findByPromocionId(Long promocionId);

    /**
     * Saber en qué promos aparece un producto.
     * Útil para validar antes de eliminar o modificar un producto.
     */
    List<PromocionItem> findByProductoId(Long productoId);

    /**
     * Verificar si ya existe el producto en la promo (evita duplicados).
     */
    boolean existsByPromocionIdAndProductoId(Long promocionId, Long productoId);

    /**
     * Borrar todos los items de una promo.
     * Se usa al actualizar la lista completa de items.
     */
    @Modifying
    @Query("DELETE FROM PromocionItem pi WHERE pi.promocion.id = :promocionId")
    void deleteByPromocionId(@Param("promocionId") Long promocionId);
}