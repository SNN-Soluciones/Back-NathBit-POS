// src/main/java/com/snnsoluciones/backnathbitpos/repository/V2SesionPlataformaRepository.java

package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.V2SesionPlataforma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface V2SesionPlataformaRepository extends JpaRepository<V2SesionPlataforma, Long> {

    List<V2SesionPlataforma> findBySesionId(Long sesionId);
}