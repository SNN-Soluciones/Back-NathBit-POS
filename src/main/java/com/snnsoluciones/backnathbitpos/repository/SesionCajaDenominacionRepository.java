// SesionCajaDenominacionRepository.java
package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.SesionCajaDenominacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SesionCajaDenominacionRepository extends JpaRepository<SesionCajaDenominacion, Long> {
  List<SesionCajaDenominacion> findBySesionCajaId(Long sesionId);
}