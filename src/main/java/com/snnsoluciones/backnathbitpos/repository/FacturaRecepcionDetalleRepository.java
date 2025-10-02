package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FacturaRecepcionDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacturaRecepcionDetalleRepository extends JpaRepository<FacturaRecepcionDetalle, Long> {
    // Métodos adicionales cuando se necesiten
}