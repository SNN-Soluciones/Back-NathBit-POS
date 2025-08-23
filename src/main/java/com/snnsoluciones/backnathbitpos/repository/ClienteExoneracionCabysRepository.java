package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ClienteExoneracionCabys;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClienteExoneracionCabysRepository extends JpaRepository<ClienteExoneracionCabys, Long> {

  // navega la relación: cabys.codigo
  boolean existsByExoneracionIdAndCabys_Codigo(Long exoneracionId, String codigo);

  // utilidades opcionales
  int countByExoneracionId(Long exoneracionId);

  List<ClienteExoneracionCabys> findAllByExoneracionId(Long exoneracionId);
}