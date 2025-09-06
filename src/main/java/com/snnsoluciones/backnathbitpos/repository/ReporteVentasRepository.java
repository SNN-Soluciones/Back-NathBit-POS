package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteVentasLineaDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository específico para reportes de ventas
 * Usa queries nativas optimizadas para performance
 */
@Repository
public interface ReporteVentasRepository extends JpaRepository<Factura, Long> {

    /**
     * Query principal para reporte de ventas
     * Une facturas con actividad económica principal
     * Las notas de crédito se mostrarán con valores positivos (se negarán en el DTO)
     */
    @Query(value = """
        SELECT 
            f.clave,
            f.consecutivo,
            f.tipo_documento as tipoDocumento,
            f.fecha_emision as fechaEmision,
            ae.codigo as actividadEconomicaCodigo,
            ae.descripcion as actividadEconomicaDescripcion,
            COALESCE(c.razon_social, 'CLIENTE GENERICO') as clienteNombre,
            COALESCE(c.numero_identificacion, '000000000') as clienteIdentificacion,
            COALESCE(c.tipo_identificacion, 'OTRO') as clienteTipoIdentificacion,
            f.total_mercancias_gravadas as totalMercanciasGravadas,
            f.total_mercancias_exentas as totalMercanciasExentas,
            f.total_mercancias_exoneradas as totalMercanciasExoneradas,
            f.total_servicios_gravados as totalServiciosGravados,
            f.total_servicios_exentos as totalServiciosExentos,
            f.total_servicios_exonerados as totalServiciosExonerados,
            f.total_venta_neta as totalVentaNeta,
            f.total_impuesto as totalImpuesto,
            f.total_descuentos as totalDescuentos,
            COALESCE(f.monto_total_impuesto_exonerado, 0) as montoTotalExonerado,
            f.total_otros_cargos as totalOtrosCargos,
            f.total_comprobante as totalComprobante,
            f.moneda,
            f.tipo_cambio as tipoCambio,
            f.estado
        FROM facturas f
        INNER JOIN sucursales s ON f.sucursal_id = s.id
        LEFT JOIN empresas_actividades ea ON ea.empresa_id = s.empresa_id AND ea.es_principal = true
        LEFT JOIN actividades_economicas ae ON ea.actividad_economica_id = ae.id
        LEFT JOIN clientes c ON f.cliente_id = c.id
        WHERE f.sucursal_id = :sucursalId
        AND f.fecha_emision >= :fechaDesde
        AND f.fecha_emision <= :fechaHasta
        AND f.estado IN ('ACEPTADA', 'GENERADA')
        AND f.tipo_documento IN ('FACTURA_ELECTRONICA', 'TIQUETE_ELECTRONICO', 'NOTA_CREDITO')
        ORDER BY f.fecha_emision ASC, f.consecutivo ASC
        """, nativeQuery = true)
    List<ReporteVentasLineaDTO> obtenerDatosReporteVentas(
        @Param("sucursalId") Long sucursalId,
        @Param("fechaDesde") LocalDateTime fechaDesde,
        @Param("fechaHasta") LocalDateTime fechaHasta
    );

    /**
     * Query para obtener solo exoneradas (para reportes futuros)
     */
    @Query(value = """
        SELECT * FROM (
            SELECT 
                f.clave,
                f.consecutivo,
                f.tipo_documento as tipoDocumento,
                f.fecha_emision as fechaEmision,
                ae.codigo as actividadEconomicaCodigo,
                ae.descripcion as actividadEconomicaDescripcion,
                COALESCE(c.razon_social, 'CLIENTE GENERICO') as clienteNombre,
                COALESCE(c.numero_identificacion, '000000000') as clienteIdentificacion,
                COALESCE(c.tipo_identificacion, 'OTRO') as clienteTipoIdentificacion,
                f.total_mercancias_gravadas as totalMercanciasGravadas,
                f.total_mercancias_exentas as totalMercanciasExentas,
                f.total_mercancias_exoneradas as totalMercanciasExoneradas,
                f.total_servicios_gravados as totalServiciosGravados,
                f.total_servicios_exentos as totalServiciosExentos,
                f.total_servicios_exonerados as totalServiciosExonerados,
                f.total_venta_neta as totalVentaNeta,
                f.total_impuesto as totalImpuesto,
                f.total_descuentos as totalDescuentos,
                COALESCE(f.monto_total_impuesto_exonerado, 0) as montoTotalExonerado,
                f.total_otros_cargos as totalOtrosCargos,
                f.total_comprobante as totalComprobante,
                f.moneda,
                f.tipo_cambio as tipoCambio,
                f.estado
            FROM facturas f
            INNER JOIN sucursales s ON f.sucursal_id = s.id
            LEFT JOIN empresas_actividades ea ON ea.empresa_id = s.empresa_id AND ea.es_principal = true
            LEFT JOIN actividades_economicas ae ON ea.actividad_economica_id = ae.id
            LEFT JOIN clientes c ON f.cliente_id = c.id
            WHERE f.sucursal_id = :sucursalId
            AND f.fecha_emision >= :fechaDesde
            AND f.fecha_emision <= :fechaHasta
            AND f.estado IN ('ACEPTADA', 'GENERADA')
            AND f.tipo_documento IN ('FACTURA_ELECTRONICA', 'TIQUETE_ELECTRONICO', 'NOTA_CREDITO')
        ) datos
        WHERE (totalMercanciasExoneradas > 0 OR totalServiciosExonerados > 0 OR montoTotalExonerado > 0)
        ORDER BY fechaEmision ASC, consecutivo ASC
        """, nativeQuery = true)
    List<ReporteVentasLineaDTO> obtenerVentasExoneradas(
        @Param("sucursalId") Long sucursalId,
        @Param("fechaDesde") LocalDateTime fechaDesde,
        @Param("fechaHasta") LocalDateTime fechaHasta
    );
}