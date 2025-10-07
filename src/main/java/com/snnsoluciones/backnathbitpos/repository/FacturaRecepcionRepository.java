package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FacturaRecepcion;
import com.snnsoluciones.backnathbitpos.enums.factura.EstadoFacturaRecepcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaRecepcionRepository extends JpaRepository<FacturaRecepcion, Long> {

  Optional<FacturaRecepcion> findByClave(String clave);

  boolean existsByClave(String clave);

  List<FacturaRecepcion> findByEmpresaIdAndSucursalIdAndEstadoInternoOrderByFechaRecepcionDesc(
      Long empresaId,
      Long sucursalId,
      EstadoFacturaRecepcion estadoInterno
  );

  @Query("""
          SELECT fr FROM FacturaRecepcion fr
          WHERE fr.empresa.id = :empresaId
          AND (:sucursalId IS NULL OR fr.sucursal.id = :sucursalId)
          AND (:estado IS NULL OR fr.estadoInterno = :estadoInterno)
          AND fr.fechaEmision >= COALESCE(:fechaInicio, fr.fechaEmision)
          AND fr.fechaEmision <= COALESCE(:fechaFin, fr.fechaEmision)
          ORDER BY fr.fechaRecepcion DESC
      """)
  List<FacturaRecepcion> findByFiltros(
      @Param("empresaId") Long empresaId,
      @Param("sucursalId") Long sucursalId,
      @Param("estadoInterno") EstadoFacturaRecepcion estadoInterno,
      @Param("fechaDesde") LocalDateTime fechaDesde,
      @Param("fechaHasta") LocalDateTime fechaHasta
  );

  long countByEmpresaIdAndSucursalIdAndEstadoInterno(
      Long empresaId,
      Long sucursalId,
      EstadoFacturaRecepcion estadoInterno
  );
}