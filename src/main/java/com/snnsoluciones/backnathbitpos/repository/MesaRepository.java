// MesaRepository.java
package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Mesa;
import com.snnsoluciones.backnathbitpos.enums.EstadoMesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MesaRepository extends JpaRepository<Mesa, Long> {
  List<Mesa> findByZonaIdOrderByOrdenAsc(Long zonaId);
  Optional<Mesa> findByZonaIdAndCodigo(Long zonaId, String codigo);
  long countByZonaIdAndEstado(Long zonaId, EstadoMesa estado);
}