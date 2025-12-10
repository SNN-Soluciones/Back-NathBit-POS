package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ClienteExoneracionCabys;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteExoneracionCabysRepository extends JpaRepository<ClienteExoneracionCabys, Long> {

  // navega la relación: cabys.codigo
  boolean existsByExoneracionIdAndCabys_Codigo(Long exoneracionId, String codigo);

  @Modifying
  @Query("DELETE FROM ClienteExoneracionCabys c WHERE c.exoneracion.id = :exoneracionId")
  void deleteAllByExoneracionId(@Param("exoneracionId") Long exoneracionId);
}