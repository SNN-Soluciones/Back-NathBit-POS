package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FacturaBitacora;
import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para gestión de bitácora de facturación electrónica
 */
@Repository
public interface FacturaBitacoraRepository extends JpaRepository<FacturaBitacora, Long>,
    JpaSpecificationExecutor<FacturaBitacora> {

  @Query("SELECT b FROM FacturaBitacora b WHERE " +
      "(b.estado = 'PENDIENTE' OR b.estado = 'ERROR') AND " +
      "(b.proximoIntento IS NULL OR b.proximoIntento <= :ahora) AND " +
      "b.intentos < 3 ORDER BY b.createdAt")
  List<FacturaBitacora> findFacturasPendientesProcesar(
      @Param("ahora") LocalDateTime ahora,
      Pageable pageable
  );

  Optional<FacturaBitacora> findByClave(String clave);

  Optional<FacturaBitacora> findByFacturaId(Long facturaId);

  @Query("SELECT COUNT(b) FROM FacturaBitacora b WHERE b.estado = :estado")
  Long countByEstado(@Param("estado") EstadoBitacora estado);

  @Query("SELECT b FROM FacturaBitacora b WHERE " +
      "b.createdAt >= :fechaInicio AND b.createdAt <= :fechaFin")
  List<FacturaBitacora> findByPeriodo(
      @Param("fechaInicio") LocalDateTime fechaInicio,
      @Param("fechaFin") LocalDateTime fechaFin
  );

}