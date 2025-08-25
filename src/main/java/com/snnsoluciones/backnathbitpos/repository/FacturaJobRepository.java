package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.entity.FacturaJob;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoProcesoJob;
import com.snnsoluciones.backnathbitpos.enums.facturacion.PasoFacturacion;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FacturaJobRepository extends JpaRepository<FacturaJob, Long> {

    Optional<FacturaJob> findByClave(String clave);

    boolean existsByFacturaId(Long facturaId);

    Optional<FacturaJob> findByFacturaId(Long facturaId);

    // Jobs elegibles globales
    @Query("""
        SELECT j
          FROM FacturaJob j
         WHERE j.estadoProceso IN (:estados)
           AND j.proximaEjecucion <= CURRENT_TIMESTAMP
           AND j.intentos < :maxIntentos
      ORDER BY j.proximaEjecucion ASC
        """)
    List<FacturaJob> findJobsPendientes(@Param("estados") List<EstadoProcesoJob> estados,
                                        @Param("maxIntentos") int maxIntentos,
                                        Pageable pageable);

    @Query("SELECT j FROM FacturaJob j WHERE j.estadoProceso IN ('PENDIENTE','REINTENTANDO') " +
        "AND j.intentos < 5 ORDER BY j.proximaEjecucion ASC")
    List<FacturaJob> findJobsPendientes(Pageable pageable);

    // Jobs por pasos (para workers por etapa)
    @Query("""
        SELECT j
          FROM FacturaJob j
         WHERE j.estadoProceso IN (:estados)
           AND j.pasoActual IN (:pasos)
           AND j.proximaEjecucion <= CURRENT_TIMESTAMP
           AND j.intentos < :maxIntentos
      ORDER BY j.proximaEjecucion ASC
        """)
    List<FacturaJob> findJobsPendientesPorPasos(@Param("estados") List<EstadoProcesoJob> estados,
                                                @Param("pasos") Set<PasoFacturacion> pasos,
                                                @Param("maxIntentos") int maxIntentos,
                                                Pageable pageable);

    // Para “claim” con lock pesimista (opcional según tu estrategia)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM FacturaJob j WHERE j.id = :id")
    Optional<FacturaJob> findByIdForUpdate(@Param("id") Long id);

    @Query("""
   SELECT j 
   FROM FacturaJob j
   WHERE j.estadoProceso IN ('PENDIENTE','REINTENTANDO')
     AND j.intentos < 5
     AND j.pasoActual IN :pasos
   ORDER BY j.proximaEjecucion ASC
   """)
    List<FacturaJob> findJobsPendientesPorPasos(@Param("pasos") Collection<PasoFacturacion> pasos);
}