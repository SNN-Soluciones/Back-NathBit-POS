package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FacturaInternaMediosPago;
import com.snnsoluciones.backnathbitpos.enums.mh.MedioPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacturaInternaMediosPagoRepository extends JpaRepository<FacturaInternaMediosPago, Long> {
    
    List<FacturaInternaMediosPago> findByFacturaId(Long facturaId);
    
    List<FacturaInternaMediosPago> findByFacturaIdAndTipoPago(Long facturaId, MedioPago tipoPago);
}