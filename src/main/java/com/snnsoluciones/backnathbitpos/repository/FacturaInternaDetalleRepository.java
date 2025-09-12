package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FacturaInternaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacturaInternaDetalleRepository extends JpaRepository<FacturaInternaDetalle, Long> {
    
    List<FacturaInternaDetalle> findByFacturaIdOrderByNumeroLinea(Long facturaId);
    
    void deleteByFacturaId(Long facturaId);
}