// ZonaMesaRepository.java
package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ZonaMesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZonaMesaRepository extends JpaRepository<ZonaMesa, Long> {
  List<ZonaMesa> findBySucursalIdAndActivoTrueOrderByOrdenAsc(Long sucursalId);
}