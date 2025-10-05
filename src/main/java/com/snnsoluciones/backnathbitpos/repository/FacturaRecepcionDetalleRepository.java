package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FacturaRecepcionDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacturaRecepcionDetalleRepository extends JpaRepository<FacturaRecepcionDetalle, Long> {

    List<FacturaRecepcionDetalle> findByFacturaRecepcionIdOrderByNumeroLineaAsc(Long facturaRecepcionId);
}