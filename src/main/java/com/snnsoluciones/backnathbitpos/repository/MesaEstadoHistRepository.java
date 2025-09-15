// MesaEstadoHistRepository.java
package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.MesaEstadoHist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MesaEstadoHistRepository extends JpaRepository<MesaEstadoHist, Long> {
  List<MesaEstadoHist> findTop20ByMesaIdOrderByFechaCambioDesc(Long mesaId);
}