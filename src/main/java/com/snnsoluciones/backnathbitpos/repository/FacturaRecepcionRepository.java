package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FacturaRecepcion;
import com.snnsoluciones.backnathbitpos.entity.FacturaRecepcionDetalle;
import com.snnsoluciones.backnathbitpos.enums.factura.EstadoFacturaRecepcion;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaRecepcionRepository extends JpaRepository<FacturaRecepcion, Long> {

  List<FacturaRecepcion> findByEmpresaIdAndFechaEmisionBetween(
      Long empresaId,
      LocalDateTime fechaDesde,
      LocalDateTime fechaHasta
  );

  Optional<FacturaRecepcion> findByClave(String clave);

  boolean existsByClave(String clave);

  List<FacturaRecepcion> findByEmpresaIdAndSucursalIdAndEstadoInternoOrderByFechaRecepcionDesc(
      Long empresaId,
      Long sucursalId,
      EstadoFacturaRecepcion estadoInterno
  );

  @Query(value = """
    SELECT * FROM facturas_recepcion fr
    WHERE fr.empresa_id = :empresaId
    AND (:sucursalId IS NULL OR fr.sucursal_id = :sucursalId)
    AND (CAST(:estado AS varchar) IS NULL OR fr.estado_interno = CAST(:estado AS varchar))
    AND (
        (:fechaInicio IS NULL AND :fechaFin IS NULL) 
        OR 
        fr.fecha_emision::date BETWEEN COALESCE(CAST(:fechaInicio AS date), '1900-01-01') 
                                   AND COALESCE(CAST(:fechaFin AS date), '2099-12-31')
    )
    ORDER BY fr.fecha_recepcion DESC
""",
      countQuery = """
        SELECT COUNT(*) FROM facturas_recepcion fr
        WHERE fr.empresa_id = :empresaId
        AND (:sucursalId IS NULL OR fr.sucursal_id = :sucursalId)
        AND (CAST(:estado AS varchar) IS NULL OR fr.estado_interno = CAST(:estado AS varchar))
        AND (
            (:fechaInicio IS NULL AND :fechaFin IS NULL) 
            OR 
            fr.fecha_emision::date BETWEEN COALESCE(CAST(:fechaInicio AS date), '1900-01-01') 
                                       AND COALESCE(CAST(:fechaFin AS date), '2099-12-31')
        )
    """,
      nativeQuery = true)
  Page<FacturaRecepcion> findByFiltros(
      @Param("empresaId") Long empresaId,
      @Param("sucursalId") Long sucursalId,
      @Param("estado") String estado,
      @Param("fechaInicio") LocalDate fechaInicio,
      @Param("fechaFin") LocalDate fechaFin,
      Pageable pageable
  );

  long countByEmpresaIdAndSucursalIdAndEstadoInterno(
      Long empresaId,
      Long sucursalId,
      EstadoFacturaRecepcion estadoInterno
  );

  /**
   * Buscar facturas ACEPTADAS en rango de fechas para reporte Excel SIN EAGER FETCH DE MÚLTIPLES
   * BAGS para evitar MultipleBagFetchException
   *
   * @param fechaInicio Inicio del rango (inclusive)
   * @param fechaFin    Fin del rango (inclusive)
   * @return Lista de facturas ordenadas por fecha de emisión
   */
  @Query("""
      SELECT DISTINCT fr FROM FacturaRecepcion fr
      LEFT JOIN FETCH fr.detalles
      WHERE fr.estadoInterno = 'ACEPTADA'
      AND fr.fechaEmision >= :fechaInicio
      AND fr.fechaEmision <= :fechaFin
      ORDER BY fr.fechaEmision ASC
      """)
  List<FacturaRecepcion> findAceptadasParaReporte(
      @Param("fechaInicio") LocalDateTime fechaInicio,
      @Param("fechaFin") LocalDateTime fechaFin
  );

  /**
   * Cargar impuestos de detalles en query separada
   * Se ejecuta DESPUÉS de obtener las facturas con detalles
   *
   * @param facturaIds IDs de las facturas a cargar impuestos
   * @return Lista de detalles con impuestos cargados (se descarta, solo carga en sesión)
   */
  @Query("""
  SELECT DISTINCT d FROM FacturaRecepcionDetalle d
  LEFT JOIN FETCH d.impuestos i
  WHERE d.facturaRecepcion.id IN :facturaIds
  """)
  List<FacturaRecepcionDetalle> cargarImpuestosDeDetalles(@Param("facturaIds") List<Long> facturaIds);
}