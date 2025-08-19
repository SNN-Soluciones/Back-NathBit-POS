package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.OtroCargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OtroCargoRepository extends JpaRepository<OtroCargo, Long> {
    
    /**
     * Buscar otros cargos por factura
     */
    List<OtroCargo> findByFacturaIdOrderByNumeroLinea(Long facturaId);
    
    /**
     * Buscar por tipo de documento
     */
    List<OtroCargo> findByFacturaIdAndTipoDocumentoOC(Long facturaId, String tipoDocumentoOC);
    
    /**
     * Contar otros cargos de una factura
     */
    Long countByFacturaId(Long facturaId);
    
    /**
     * Calcular total de otros cargos por factura
     */
    @Query("SELECT COALESCE(SUM(oc.montoCargo), 0) FROM OtroCargo oc WHERE oc.factura.id = :facturaId")
    BigDecimal calcularTotalPorFactura(@Param("facturaId") Long facturaId);
    
    /**
     * Buscar cargos de servicio 10%
     */
    @Query("SELECT oc FROM OtroCargo oc WHERE oc.factura.id = :facturaId AND oc.tipoDocumentoOC = '06'")
    List<OtroCargo> findServiciosByFacturaId(@Param("facturaId") Long facturaId);
    
    /**
     * Verificar si existe un tipo de cargo específico
     */
    boolean existsByFacturaIdAndTipoDocumentoOC(Long facturaId, String tipoDocumentoOC);
}