package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.CierreDatafono;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CierreDatafonoRepository extends JpaRepository<CierreDatafono, Long> {

    /**
     * Busca todos los datafonos de una sesión específica
     */
    List<CierreDatafono> findBySesionCajaId(Long sesionCajaId);

    /**
     * Calcula el total de datafonos de una sesión
     */
    @Query("SELECT COALESCE(SUM(cd.monto), 0) FROM CierreDatafono cd WHERE cd.sesionCaja.id = :sesionId")
    BigDecimal sumTotalDatafonosBySesionId(@Param("sesionId") Long sesionId);

    /**
     * Elimina todos los datafonos de una sesión (útil para recalcular)
     */
    void deleteBySesionCajaId(Long sesionCajaId);
}