package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ClienteExoneracionCabys;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteExoneracionCabysRepository extends JpaRepository<ClienteExoneracionCabys, Long> {

  // navega la relación: cabys.codigo
  boolean existsByExoneracionIdAndCabys_Codigo(Long exoneracionId, String codigo);
}