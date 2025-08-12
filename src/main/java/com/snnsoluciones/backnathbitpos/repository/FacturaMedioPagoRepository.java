package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FacturaMedioPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Repository simple - los medios de pago se manejan desde Factura
@Repository
public interface FacturaMedioPagoRepository extends JpaRepository<FacturaMedioPago, Long> {
    // Los medios de pago se guardan en cascada desde Factura
    // No necesitamos queries especiales aquí
}