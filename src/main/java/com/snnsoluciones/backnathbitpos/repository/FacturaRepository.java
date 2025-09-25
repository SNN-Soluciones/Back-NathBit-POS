package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
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

  Optional<Factura> findByConsecutivo(String consecutivo);

  boolean existsByClave(String clave);

  List<Factura> findAllByEstado(EstadoFactura estado);

  boolean existsByConsecutivo(String consecutivo);

  // Carga “full” para generar XML sin N+1 (ajusta relaciones según tu modelo)
  @Query("SELECT f FROM Factura f " +
      "LEFT JOIN FETCH f.cliente " +
      "LEFT JOIN FETCH f.sucursal s " +
      "LEFT JOIN FETCH s.empresa " +
      "LEFT JOIN FETCH f.detalles d " +
      "LEFT JOIN FETCH d.producto " +
      "WHERE f.id = :id")
  Optional<Factura> findByIdWithRelaciones(@Param("id") Long id);

  // Lock optimista opcional cuando actualices totales/estado
  @Lock(LockModeType.OPTIMISTIC)
  @Query("SELECT f FROM Factura f WHERE f.id = :id")
  Optional<Factura> findByIdForUpdate(@Param("id") Long id);

  List<Factura> findBySucursalIdAndEstado(Long sucursalId, EstadoFactura estado);

  /**
   * Buscar facturas por sesión de caja
   */
  @Query("SELECT f FROM Factura f WHERE f.sesionCaja.id = :sesionId ORDER BY f.fechaEmision DESC")
  List<Factura> findBySesionCajaId(@Param("sesionId") Long sesionId);

  /**
   * Buscar facturas por sesión de caja con paginación
   */
  Page<Factura> findBySesionCajaIdOrderByFechaEmisionDesc(Long sesionCajaId, Pageable pageable);

  /**
   * Contar facturas por sesión y tipo de documento
   */
  @Query("SELECT COUNT(f) FROM Factura f WHERE f.sesionCaja.id = :sesionId AND f.tipoDocumento = :tipo")
  Long countBySesionIdAndTipoDocumento(@Param("sesionId") Long sesionId, @Param("tipo") TipoDocumento tipo);

  /**
   * Sumar totales por sesión y tipo de documento
   */
  @Query("SELECT COALESCE(SUM(f.totalComprobante), 0) FROM Factura f WHERE f.sesionCaja.id = :sesionId AND f.tipoDocumento = :tipo AND f.estado NOT IN ('ANULADA', 'RECHAZADA')")
  BigDecimal sumTotalBySesionIdAndTipoDocumento(@Param("sesionId") Long sesionId, @Param("tipo") TipoDocumento tipo);

  /**
   * Obtener resumen de facturas por sesión
   */
  @Query("""
    SELECT new map(
        f.tipoDocumento as tipoDocumento,
        COUNT(f) as cantidad,
        SUM(f.totalComprobante) as total
    )
    FROM Factura f 
    WHERE f.sesionCaja.id = :sesionId 
    AND f.estado NOT IN ('ANULADA', 'RECHAZADA')
    GROUP BY f.tipoDocumento
""")
  List<Map<String, Object>> obtenerResumenPorTipoDocumento(@Param("sesionId") Long sesionId);

  /**
   * Buscar facturas por sesión, tipo y estado
   */
  List<Factura> findBySesionCajaIdAndTipoDocumentoAndEstado(
      Long sesionCajaId,
      TipoDocumento tipoDocumento,
      EstadoFactura estado
  );

  /**
   * Obtener total de ventas efectivas (sin NC) de una sesión
   */
  @Query("""
    SELECT COALESCE(SUM(f.totalComprobante), 0) 
    FROM Factura f 
    WHERE f.sesionCaja.id = :sesionId 
    AND f.tipoDocumento IN ('FACTURA_ELECTRONICA', 'TIQUETE_ELECTRONICO', 'FACTURA_INTERNA', 'TIQUETE_INTERNO')
    AND f.estado NOT IN ('ANULADA', 'RECHAZADA')
""")
  BigDecimal obtenerTotalVentasSesion(@Param("sesionId") Long sesionId);

  /**
   * Obtener total de devoluciones (NC) de una sesión
   */
  @Query("""
    SELECT COALESCE(SUM(f.totalComprobante), 0) 
    FROM Factura f 
    WHERE f.sesionCaja.id = :sesionId 
    AND f.tipoDocumento = 'NOTA_CREDITO'
    AND f.estado NOT IN ('ANULADA', 'RECHAZADA')
""")
  BigDecimal obtenerTotalDevolucionesSesion(@Param("sesionId") Long sesionId);
}