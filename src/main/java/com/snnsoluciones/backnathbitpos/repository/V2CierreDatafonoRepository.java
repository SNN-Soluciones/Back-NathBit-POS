// src/main/java/com/snnsoluciones/backnathbitpos/repository/V2CierreDatafonoRepository.java

package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.V2CierreDatafono;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface V2CierreDatafonoRepository extends JpaRepository<V2CierreDatafono, Long> {

    List<V2CierreDatafono> findBySesionId(Long sesionId);

    List<V2CierreDatafono> findByTurnoId(Long turnoId);
}