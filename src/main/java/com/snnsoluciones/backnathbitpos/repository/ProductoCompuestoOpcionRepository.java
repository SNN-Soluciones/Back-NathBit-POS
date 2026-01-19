// ProductoCompuestoOpcionRepository.java
package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ProductoCompuestoOpcion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoCompuestoOpcionRepository extends JpaRepository<ProductoCompuestoOpcion, Long> {
    List<ProductoCompuestoOpcion> findBySlotId(Long slotId);
    Optional<ProductoCompuestoOpcion> findById(Long id);

    @Query("""
    SELECT o
    FROM ProductoCompuestoOpcion o
    LEFT JOIN FETCH o.producto p
    WHERE o.slot.id = :slotId
    ORDER BY o.orden
  """)
    List<ProductoCompuestoOpcion> findBySlotIdOrderByOrdenWithProducto(@Param("slotId") Long slotId);

}