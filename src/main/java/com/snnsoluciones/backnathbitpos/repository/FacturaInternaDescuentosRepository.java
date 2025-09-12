package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FacturaInternaDescuentos;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FacturaInternaDescuentosRepository extends
    JpaRepository<FacturaInternaDescuentos, Long> {
    List<FacturaInternaDescuentos> findByFacturaId(Long facturaId);
}