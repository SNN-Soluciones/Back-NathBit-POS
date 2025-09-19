package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FacturaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Repository simple - los detalles se manejan principalmente desde Factura
@Repository
public interface FacturaDetalleRepository extends JpaRepository<FacturaDetalle, Long> {

  boolean existsByProductoId(Long id);
  // Los detalles se guardan en cascada desde Factura
    // No necesitamos queries especiales aquí
}