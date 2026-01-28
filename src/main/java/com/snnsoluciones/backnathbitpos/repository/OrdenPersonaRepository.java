package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.OrdenPersona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdenPersonaRepository extends JpaRepository<OrdenPersona, Long> {

    /**
     * Busca todas las personas de una orden
     */
    List<OrdenPersona> findByOrdenIdOrderByOrdenVisualizacionAsc(Long ordenId);

    /**
     * Busca una persona por nombre en una orden específica
     */
    Optional<OrdenPersona> findByOrdenIdAndNombre(Long ordenId, String nombre);

    /**
     * Verifica si existe una persona con ese nombre en la orden
     */
    boolean existsByOrdenIdAndNombre(Long ordenId, String nombre);

    /**
     * Cuenta cuántas personas tiene una orden
     */
    long countByOrdenId(Long ordenId);

    /**
     * Busca personas activas de una orden
     */
    List<OrdenPersona> findByOrdenIdAndActivoTrueOrderByOrdenVisualizacionAsc(Long ordenId);
}