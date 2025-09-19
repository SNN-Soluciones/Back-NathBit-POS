// ProductoCompuestoOpcionRepository.java
package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ProductoCompuestoOpcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoCompuestoOpcionRepository extends JpaRepository<ProductoCompuestoOpcion, Long> {
    List<ProductoCompuestoOpcion> findBySlotIdAndDisponibleTrue(Long slotId);
    List<ProductoCompuestoOpcion> findBySlotId(Long slotId);
    void deleteBySlotId(Long slotId);
}