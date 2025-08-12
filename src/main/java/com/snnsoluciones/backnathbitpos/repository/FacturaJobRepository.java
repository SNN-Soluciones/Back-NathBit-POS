package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FacturaJob;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoProcesoJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaJobRepository extends JpaRepository<FacturaJob, Long> {
    
    Optional<FacturaJob> findByClave(String clave);
    
    // Jobs listos para procesar
    @Query("SELECT j FROM FacturaJob j WHERE j.estadoProceso IN ('PENDIENTE', 'REINTENTANDO') " +
           "AND j.proximaEjecucion <= :ahora AND j.intentos < 5 " +
           "ORDER BY j.proximaEjecucion ASC")
    List<FacturaJob> findJobsPendientes(@Param("ahora") LocalDateTime ahora);
    
    // Para verificar si ya existe un job para una factura
    boolean existsByFacturaId(Long facturaId);
}