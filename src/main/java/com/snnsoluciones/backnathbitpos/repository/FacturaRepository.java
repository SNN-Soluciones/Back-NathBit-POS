package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.entity.FacturaDetalle;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long>,
    JpaSpecificationExecutor<Factura> {

  Optional<Factura> findByClave(String clave);

  @Query("""
      select f
      from Factura f
        join fetch f.sucursal s
        join fetch s.empresa e
      where f.id in :ids
    """)
  List<Factura> findAllByIdInWithEmpresa(Collection<Long> ids);

  Optional<Factura> findByConsecutivo(String consecutivo);

  // Carga “full” para generar XML sin N+1 (ajusta relaciones según tu modelo)
  @Query("SELECT f FROM Factura f " +
      "LEFT JOIN FETCH f.cliente " +
      "LEFT JOIN FETCH f.sucursal s " +
      "LEFT JOIN FETCH s.empresa " +
      "LEFT JOIN FETCH f.detalles d " +
      "LEFT JOIN FETCH d.producto " +
      "WHERE f.id = :id")
  Optional<Factura> findByIdWithRelaciones(@Param("id") Long id);

  List<Factura> findBySucursalIdAndEstado(Long sucursalId, EstadoFactura estado);

  /**
   * Buscar facturas por sesión de caja
   */
  @Query("SELECT f FROM Factura f WHERE f.sesionCaja.id = :sesionId ORDER BY f.fechaEmision DESC")
  List<Factura> findBySesionCajaId(@Param("sesionId") Long sesionId);

  // Agregar estos métodos a FacturaRepository.java

  /**
   * Buscar facturas de VENTA en rango de fechas para reporte Hacienda
   * SIN EAGER FETCH DE MÚLTIPLES BAGS para evitar MultipleBagFetchException
   *
   * @param empresaId ID de la empresa
   * @param sucursalId ID de la sucursal
   * @param fechaInicio Inicio del rango (inclusive)
   * @param fechaFin Fin del rango (inclusive)
   * @return Lista de facturas ordenadas por fecha de emisión
   */
  @Query("""
  SELECT DISTINCT f FROM Factura f
  LEFT JOIN FETCH f.detalles
  WHERE f.sucursal.empresa.id = :empresaId
  AND f.sucursal.id = :sucursalId
  AND f.fechaEmision >= :fechaInicio
  AND f.fechaEmision <= :fechaFin
  AND f.tipoDocumento IN ('FACTURA_ELECTRONICA', 'TIQUETE_ELECTRONICO', 'NOTA_CREDITO', 'NOTA_DEBITO')
  ORDER BY f.fechaEmision ASC
  """)
  List<Factura> findVentasParaReporte(
      @Param("empresaId") Long empresaId,
      @Param("sucursalId") Long sucursalId,
      @Param("fechaInicio") LocalDateTime fechaInicio,
      @Param("fechaFin") LocalDateTime fechaFin
  );

  /**
   * Cargar impuestos de detalles de facturas en query separada
   * Se ejecuta DESPUÉS de obtener las facturas con detalles
   *
   * @param facturaIds IDs de las facturas a cargar impuestos
   * @return Lista de detalles con impuestos cargados (se descarta, solo carga en sesión)
   */
  @Query("""
  SELECT DISTINCT d FROM FacturaDetalle d
  LEFT JOIN FETCH d.impuestos i
  WHERE d.factura.id IN :facturaIds
  """)
  List<FacturaDetalle> cargarImpuestosDeDetalles(@Param("facturaIds") List<Long> facturaIds);

  @org.springframework.data.jpa.repository.Query("""
    select f
    from Factura f
      join fetch f.sucursal s
      join fetch s.empresa e
    where f.id = :id
""")
  java.util.Optional<Factura> findByIdFetchEmpresa(@org.springframework.data.repository.query.Param("id") Long id);

}