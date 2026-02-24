package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.OpcionV2;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OpcionV2Repository extends JpaRepository<OpcionV2, Long> {
    List<OpcionV2> findBySlotIdOrderByOrden(Long slotId);
}