package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.PromocionFamilia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromocionFamiliaRepository extends JpaRepository<PromocionFamilia, Long> {

    List<PromocionFamilia> findByPromocionId(Long promocionId);

    /** Saber en qué promos está incluida una familia. */
    List<PromocionFamilia> findByFamiliaId(Long familiaId);

    boolean existsByPromocionIdAndFamiliaId(Long promocionId, Long familiaId);

    /** Se usa al reemplazar el alcance completo de una promo. */
    @Modifying
    @Query("DELETE FROM PromocionFamilia pf WHERE pf.promocion.id = :promocionId")
    void deleteByPromocionId(@Param("promocionId") Long promocionId);
}