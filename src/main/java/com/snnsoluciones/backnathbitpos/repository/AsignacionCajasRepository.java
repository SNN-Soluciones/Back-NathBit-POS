package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.operacion.AsignacionCajas;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsignacionCajasRepository extends JpaRepository<AsignacionCajas, UUID> {

  void invalidarAsignacionesAnteriores(UUID userId, UUID sucursalId, UUID id);
}
