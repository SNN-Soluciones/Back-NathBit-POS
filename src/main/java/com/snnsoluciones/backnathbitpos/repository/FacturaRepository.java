package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
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

}