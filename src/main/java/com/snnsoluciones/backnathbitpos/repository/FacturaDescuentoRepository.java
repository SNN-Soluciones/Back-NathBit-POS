package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FacturaDescuento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface FacturaDescuentoRepository extends JpaRepository<FacturaDescuento, Long> {
    
    /**
     * Buscar descuentos por detalle de factura
     */
    List<FacturaDescuento> findByFacturaDetalleIdOrderByOrden(Long facturaDetalleId);
    
    /**
     * Buscar por código de descuento
     */
    List<FacturaDescuento> findByFacturaDetalleIdAndCodigoDescuento(Long facturaDetalleId, String codigoDescuento);
    
    /**
     * Contar descuentos de un detalle
     */
    Long countByFacturaDetalleId(Long facturaDetalleId);
    
    /**
     * Calcular total de descuentos por detalle
     */
    @Query("SELECT COALESCE(SUM(fd.montoDescuento), 0) FROM FacturaDescuento fd WHERE fd.facturaDetalle.id = :detalleId")
    BigDecimal calcularTotalPorDetalle(@Param("detalleId") Long detalleId);
    
    /**
     * Buscar descuentos por factura completa
     */
    @Query("SELECT fd FROM FacturaDescuento fd WHERE fd.facturaDetalle.factura.id = :facturaId ORDER BY fd.facturaDetalle.numeroLinea, fd.orden")
    List<FacturaDescuento> findByFacturaId(@Param("facturaId") Long facturaId);
    
    /**
     * Calcular total de descuentos por factura
     */
    @Query("SELECT COALESCE(SUM(fd.montoDescuento), 0) FROM FacturaDescuento fd WHERE fd.facturaDetalle.factura.id = :facturaId")
    BigDecimal calcularTotalPorFactura(@Param("facturaId") Long facturaId);
    
    /**
     * Verificar si un detalle tiene el máximo de descuentos permitidos
     */
    @Query("SELECT CASE WHEN COUNT(fd) >= 5 THEN true ELSE false END FROM FacturaDescuento fd WHERE fd.facturaDetalle.id = :detalleId")
    boolean tieneMaximoDescuentos(@Param("detalleId") Long detalleId);
}