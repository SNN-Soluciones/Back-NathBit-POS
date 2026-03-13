// SesionCajaDenominacionRepository.java
package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.SesionCajaDenominacion;
import com.snnsoluciones.backnathbitpos.enums.TipoConteoCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SesionCajaDenominacionRepository extends JpaRepository<SesionCajaDenominacion, Long> {
  List<SesionCajaDenominacion> findBySesionCajaId(Long sesionId);

  List<SesionCajaDenominacion> findBySesionCajaUsuarioId(Long sesionCajaUsuarioId);

  @Query("""
    SELECT d FROM SesionCajaDenominacion d
    WHERE d.sesionCaja.id = :sesionId
    AND d.tipoConteo = :tipoConteo
    """)
  List<SesionCajaDenominacion> findBySesionCajaIdAndTipoConteo(
      @Param("sesionId") Long sesionId,
      @Param("tipoConteo") TipoConteoCaja tipoConteo);
}