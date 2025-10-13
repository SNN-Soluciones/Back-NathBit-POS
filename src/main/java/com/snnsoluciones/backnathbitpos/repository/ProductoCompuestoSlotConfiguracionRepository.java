package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ProductoCompuestoSlotConfiguracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoCompuestoSlotConfiguracionRepository extends JpaRepository<ProductoCompuestoSlotConfiguracion, Long> {

    /**
     * Busca todos los slots de una configuración específica ordenados
     */
    List<ProductoCompuestoSlotConfiguracion> findByConfiguracionIdOrderByOrden(Long configuracionId);

    /**
     * Busca todos los slots de una configuración con el slot base cargado
     */
    @Query("SELECT sc FROM ProductoCompuestoSlotConfiguracion sc " +
           "LEFT JOIN FETCH sc.slot s " +
           "WHERE sc.configuracion.id = :configuracionId " +
           "ORDER BY sc.orden ASC")
    List<ProductoCompuestoSlotConfiguracion> findByConfiguracionIdWithSlot(@Param("configuracionId") Long configuracionId);

    /**
     * Busca la relación entre una configuración y un slot específico
     */
    Optional<ProductoCompuestoSlotConfiguracion> findByConfiguracionIdAndSlotId(
        Long configuracionId, 
        Long slotId
    );

    /**
     * Verifica si un slot está asociado a alguna configuración
     */
    boolean existsBySlotId(Long slotId);

    /**
     * Cuenta cuántas configuraciones usan un slot específico
     */
    long countBySlotId(Long slotId);

    /**
     * Busca todos los slot configs que usan un slot específico
     */
    List<ProductoCompuestoSlotConfiguracion> findBySlotId(Long slotId);

    /**
     * Busca slots config que tienen overrides activos
     */
    @Query("SELECT sc FROM ProductoCompuestoSlotConfiguracion sc " +
           "WHERE sc.configuracion.id = :configuracionId " +
           "AND (sc.cantidadMinimaOverride IS NOT NULL " +
           "     OR sc.cantidadMaximaOverride IS NOT NULL " +
           "     OR sc.esRequeridoOverride IS NOT NULL " +
           "     OR sc.precioAdicionalOverride IS NOT NULL) " +
           "ORDER BY sc.orden ASC")
    List<ProductoCompuestoSlotConfiguracion> findByConfiguracionIdWithOverrides(@Param("configuracionId") Long configuracionId);

    /**
     * Elimina todos los slots de una configuración
     */
    void deleteByConfiguracionId(Long configuracionId);

    /**
     * Elimina todas las referencias a un slot
     * (útil antes de eliminar el slot)
     */
    void deleteBySlotId(Long slotId);
}