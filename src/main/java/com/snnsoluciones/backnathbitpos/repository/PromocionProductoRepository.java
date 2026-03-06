package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.PromocionProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromocionProductoRepository extends JpaRepository<PromocionProducto, Long> {

    List<PromocionProducto> findByPromocionId(Long promocionId);

    /** Saber en qué promos aparece un producto (útil antes de eliminar un producto). */
    List<PromocionProducto> findByProductoId(Long productoId);

    boolean existsByPromocionIdAndProductoId(Long promocionId, Long productoId);

    /** Se usa al reemplazar el alcance completo de una promo. */
    @Modifying
    @Query("DELETE FROM PromocionProducto pp WHERE pp.promocion.id = :promocionId")
    void deleteByPromocionId(@Param("promocionId") Long promocionId);
}