package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FacturaInternaOtrosCargos;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FacturaInternaOtrosCargosRepository extends
    JpaRepository<FacturaInternaOtrosCargos, Long> {
    List<FacturaInternaOtrosCargos> findByFacturaId(Long facturaId);
}