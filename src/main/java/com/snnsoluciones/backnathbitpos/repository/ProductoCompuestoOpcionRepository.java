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
    select o
    from ProductoCompuestoOpcion o
    join fetch o.producto p
    where o.slot.id = :slotId
    order by o.orden
  """)
    List<ProductoCompuestoOpcion> findBySlotIdOrderByOrdenWithProducto(@Param("slotId") Long slotId);

}