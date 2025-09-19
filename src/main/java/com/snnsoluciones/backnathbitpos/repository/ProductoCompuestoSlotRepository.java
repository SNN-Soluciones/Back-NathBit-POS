// ProductoCompuestoSlotRepository.java
package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ProductoCompuestoSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoCompuestoSlotRepository extends JpaRepository<ProductoCompuestoSlot, Long> {
    List<ProductoCompuestoSlot> findByCompuestoIdOrderByOrden(Long compuestoId);
    void deleteByCompuestoId(Long compuestoId);
}