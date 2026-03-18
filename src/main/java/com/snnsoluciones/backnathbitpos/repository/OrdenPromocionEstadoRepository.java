package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.OrdenPromocionEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdenPromocionEstadoRepository extends JpaRepository<OrdenPromocionEstado, Long> {

    /**
     * Busca el estado de un producto específico dentro de una promo activa en la orden.
     * Usado en nuevaRonda() para validar si puede servirse más.
     */
    Optional<OrdenPromocionEstado> findByOrdenIdAndPromocionIdAndProductoId(
            Long ordenId, Long promocionId, Long productoId);

    /**
     * Verifica si ya existe el estado para evitar duplicados al activar AYCE.
     * Usado en activarAYCE() para garantizar idempotencia.
     */
    boolean existsByOrdenIdAndPromocionIdAndProductoId(
            Long ordenId, Long promocionId, Long productoId);

    /**
     * Devuelve todos los estados activos de una promo en una orden.
     * Usado en evaluar() para saber si un AYCE ya fue activado.
     */
    List<OrdenPromocionEstado> findByOrdenIdAndPromocionId(
            Long ordenId, Long promocionId);
}