package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FacturaInternaBitacora;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FacturaInternaBitacoraRepository extends
    JpaRepository<FacturaInternaBitacora, Long> {
    List<FacturaInternaBitacora> findByFacturaIdOrderByFechaAccionDesc(Long facturaId);
}