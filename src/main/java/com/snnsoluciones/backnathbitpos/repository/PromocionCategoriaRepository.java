package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.PromocionCategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromocionCategoriaRepository extends JpaRepository<PromocionCategoria, Long> {

    List<PromocionCategoria> findByPromocionId(Long promocionId);

    /** Saber en qué promos está incluida una categoría. */
    List<PromocionCategoria> findByCategoriaId(Long categoriaId);

    boolean existsByPromocionIdAndCategoriaId(Long promocionId, Long categoriaId);

    /** Se usa al reemplazar el alcance completo de una promo. */
    @Modifying
    @Query("DELETE FROM PromocionCategoria pc WHERE pc.promocion.id = :promocionId")
    void deleteByPromocionId(@Param("promocionId") Long promocionId);
}