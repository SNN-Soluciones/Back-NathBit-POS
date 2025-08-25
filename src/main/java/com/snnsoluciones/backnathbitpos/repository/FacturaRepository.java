package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {

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

  List<Factura> findBySesionCajaId(Long sesionCajaId);

  List<Factura> findBySucursalIdAndEstado(Long sucursalId, EstadoFactura estado);
}