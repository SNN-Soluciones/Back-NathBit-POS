package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ProductoCompuestoConfiguracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoCompuestoConfiguracionRepository extends JpaRepository<ProductoCompuestoConfiguracion, Long> {

    /**
     * Busca todas las configuraciones de un producto compuesto específico
     * ordenadas por orden
     */
    List<ProductoCompuestoConfiguracion> findByCompuestoIdOrderByOrden(Long compuestoId);

    /**
     * Busca todas las configuraciones activas de un producto compuesto
     */
    List<ProductoCompuestoConfiguracion> findByCompuestoIdAndActivaTrueOrderByOrden(Long compuestoId);

    /**
     * Busca la configuración que se activa con una opción específica
     */
    Optional<ProductoCompuestoConfiguracion> findByOpcionTriggerId(Long opcionTriggerId);

    /**
     * Busca configuraciones activas que se activan con una opción específica
     */
    @Query("SELECT c FROM ProductoCompuestoConfiguracion c " +
           "WHERE c.opcionTrigger.id = :opcionId " +
           "AND c.activa = true " +
           "ORDER BY c.orden ASC")
    List<ProductoCompuestoConfiguracion> findActivasByOpcionTriggerId(@Param("opcionId") Long opcionId);

    /**
     * Busca todas las configuraciones de un compuesto con sus slots cargados
     */
    @Query("SELECT DISTINCT c FROM ProductoCompuestoConfiguracion c " +
           "LEFT JOIN FETCH c.slots s " +
           "LEFT JOIN FETCH s.slot " +
           "WHERE c.compuesto.id = :compuestoId " +
           "ORDER BY c.orden ASC")
    List<ProductoCompuestoConfiguracion> findByCompuestoIdWithSlots(@Param("compuestoId") Long compuestoId);

    /**
     * Verifica si existe una configuración para una opción trigger específica
     */
    boolean existsByOpcionTriggerId(Long opcionTriggerId);

    /**
     * Cuenta las configuraciones activas de un compuesto
     */
    long countByCompuestoIdAndActivaTrue(Long compuestoId);

    /**
     * Elimina todas las configuraciones de un compuesto
     * (útil al eliminar el producto compuesto)
     */
    void deleteByCompuestoId(Long compuestoId);

    /**
     * Buscar configuración default de un producto compuesto
     */
    Optional<ProductoCompuestoConfiguracion> findByCompuestoIdAndEsDefaultTrue(Long compuestoId);

}