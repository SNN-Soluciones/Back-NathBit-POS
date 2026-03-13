package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.OrdenPromocionEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdenPromocionEstadoRepository extends JpaRepository<OrdenPromocionEstado, Long> {

    /**
     * Todos los estados de ronda para una orden.
     * Usado al cargar la orden en el POS para saber qué promos AYCE están activas.
     */
    List<OrdenPromocionEstado> findByOrdenId(Long ordenId);

    /**
     * Estado de un producto específico dentro de una promo y orden.
     * Usado al solicitar una nueva ronda — valida si puede servirse.
     */
    Optional<OrdenPromocionEstado> findByOrdenIdAndPromocionIdAndProductoId(
            Long ordenId, Long promocionId, Long productoId);

    /**
     * Todos los estados de una promo dentro de una orden.
     * Usado al aplicar por primera vez una promo AYCE — verifica que no esté duplicada.
     */
    List<OrdenPromocionEstado> findByOrdenIdAndPromocionId(Long ordenId, Long promocionId);

    /**
     * Verifica si ya existe un estado para esta combinación.
     * Previene doble activación de la misma promo en la misma orden.
     */
    boolean existsByOrdenIdAndPromocionIdAndProductoId(
            Long ordenId, Long promocionId, Long productoId);

    /**
     * Elimina todos los estados de una orden.
     * Usado si se cancela o anula la orden.
     */
    @Modifying
    @Query("DELETE FROM OrdenPromocionEstado e WHERE e.orden.id = :ordenId")
    void deleteByOrdenId(@Param("ordenId") Long ordenId);
}