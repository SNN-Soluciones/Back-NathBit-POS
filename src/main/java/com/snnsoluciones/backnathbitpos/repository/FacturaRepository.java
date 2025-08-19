package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {

  Optional<Factura> findByClave(String clave);

  Optional<Factura> findByConsecutivo(String consecutivo);

  // Para el cajero - facturas del día actual
  @Query("SELECT f FROM Factura f WHERE f.sesionCaja.id = :sesionId " +
      "AND f.fechaEmision >= :inicio ORDER BY f.fechaEmision DESC")
  List<Factura> findBySesionCajaHoy(@Param("sesionId") Long sesionId,
      @Param("inicio") LocalDateTime inicio);

  // Para verificar si existe consecutivo (validación)
  boolean existsByConsecutivo(String consecutivo);

  // Para dashboard - facturas con error que necesitan atención
  @Query("SELECT f FROM Factura f WHERE f.sucursal.id = :sucursalId " +
      "AND f.estado IN ('ERROR', 'RECHAZADA') " +
      "ORDER BY f.fechaEmision DESC")
  List<Factura> findFacturasConError(@Param("sucursalId") Long sucursalId);

  /**
   * Buscar facturas con error por sucursal
   *
   * @param sucursalId ID de la sucursal
   * @param estado     Estado de error
   * @return Lista de facturas en error
   */
  List<Factura> findBySucursalIdAndEstado(Long sucursalId, EstadoFactura estado);

  /**
   * Buscar facturas por sesión de caja
   *
   * @param sesionCajaId ID de la sesión de caja
   * @return Lista de facturas de esa sesión
   */
  List<Factura> findBySesionCajaId(Long sesionCajaId);
}