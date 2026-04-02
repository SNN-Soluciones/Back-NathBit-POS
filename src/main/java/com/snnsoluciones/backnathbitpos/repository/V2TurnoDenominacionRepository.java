// src/main/java/com/snnsoluciones/backnathbitpos/repository/V2TurnoDenominacionRepository.java

package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.V2TurnoDenominacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface V2TurnoDenominacionRepository extends JpaRepository<V2TurnoDenominacion, Long> {
    List<V2TurnoDenominacion> findByTurnoIdOrderByValorDesc(Long turnoId);
}