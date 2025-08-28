package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FacturaBitacora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para gestión de bitácora de facturación electrónica
 */
@Repository
public interface FacturaBitacoraRepository extends JpaRepository<FacturaBitacora, Long> {

}