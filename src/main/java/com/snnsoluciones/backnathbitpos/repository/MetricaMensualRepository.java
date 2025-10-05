package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.MetricasVentasMensuales;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetricaMensualRepository extends JpaRepository<MetricasVentasMensuales, Long> {

    // Buscar métrica específica de empresa (consolidada)
    @Query("SELECT m FROM MetricasVentasMensuales m WHERE m.empresa.id = :empresaId " +
           "AND m.sucursal IS NULL AND m.anio = :anio AND m.mes = :mes")
    Optional<MetricasVentasMensuales> findByEmpresaConsolidada(@Param("empresaId") Long empresaId,
                                                      @Param("anio") Integer anio, 
                                                      @Param("mes") Integer mes);

    // Buscar métrica específica de sucursal
    @Query("SELECT m FROM MetricasVentasMensuales m WHERE m.sucursal.id = :sucursalId " +
           "AND m.anio = :anio AND m.mes = :mes")
    Optional<MetricasVentasMensuales> findBySucursal(@Param("sucursalId") Long sucursalId,
                                           @Param("anio") Integer anio, 
                                           @Param("mes") Integer mes);

    // Obtener métricas de todas las sucursales de una empresa para un período
    @Query("SELECT m FROM MetricasVentasMensuales m WHERE m.empresa.id = :empresaId " +
           "AND m.sucursal IS NOT NULL AND m.anio = :anio AND m.mes = :mes")
    List<MetricasVentasMensuales> findAllSucursalesByEmpresaAndPeriodo(@Param("empresaId") Long empresaId,
                                                               @Param("anio") Integer anio,
                                                               @Param("mes") Integer mes);

    // Obtener histórico anual de empresa
    @Query("SELECT m FROM MetricasVentasMensuales m WHERE m.empresa.id = :empresaId " +
           "AND m.sucursal IS NULL AND m.anio = :anio ORDER BY m.mes")
    List<MetricasVentasMensuales> findHistoricoAnualEmpresa(@Param("empresaId") Long empresaId,
                                                    @Param("anio") Integer anio);

    // Query para reporte D104 - Declaración de IVA mensual
    @Query("SELECT new map(" +
        "m.ventasMh + m.ventasInternas as ventasBrutas, " +
        "m.ventasServicios as ventasServicios, " +
        "m.ventasMercancias as ventasMercancias, " +
        "m.notasCreditoTotal as notasCredito, " +
        "m.descuentosTotal as descuentos, " +
        "(m.ventasTotales - m.notasCreditoTotal - m.descuentosTotal) as ventasNetas, " +
        "m.exentoTotal as ventasExentas, " +
        "m.exoneradoTotal as ventasExoneradas, " +
        "(m.ventasTotales - m.notasCreditoTotal - m.descuentosTotal - m.exentoTotal - m.exoneradoTotal - m.impuestoTotal) as ventasGravadas, " +
        "m.impuestoIva13 as iva13, " +
        "m.impuestoIva4 as iva4, " +
        "m.impuestoIva2 as iva2, " +
        "m.impuestoIva1 as iva1, " +
        "m.impuestoTotal as totalIVA, " +
        "(m.impuestoIva13 + m.impuestoIva4 + m.impuestoIva2 + m.impuestoIva1) as ivaAcreditable, " +
        "m.cantidadFacturasMh + m.cantidadFacturasInternas as totalDocumentos, " +
        "m.cantidadNotasCredito as totalNotasCredito " +
        ") FROM MetricasVentasMensuales m " +
        "WHERE m.empresa.id = :empresaId AND m.sucursal IS NULL " +
        "AND m.anio = :anio AND m.mes = :mes")
    Map<String, Object> obtenerDatosD104(@Param("empresaId") Long empresaId,
        @Param("anio") Integer anio,
        @Param("mes") Integer mes);

}