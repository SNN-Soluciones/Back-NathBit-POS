package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.SlotV2;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SlotV2Repository extends JpaRepository<SlotV2, Long> {
    List<SlotV2> findByCompuestoIdOrderByOrden(Long compuestoId);
    void deleteByCompuestoId(Long compuestoId);
}