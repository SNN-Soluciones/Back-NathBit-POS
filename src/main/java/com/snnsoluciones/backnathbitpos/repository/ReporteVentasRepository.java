package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteVentasLineaDTO;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository específico para reportes de ventas
 * Usa queries nativas optimizadas para performance
 *
 * Nota de fechas:
 * - Se mantiene <= :fechaHasta (inclusivo). Si :fechaHasta llega sin hora (00:00),
 *   asegúrate de setearlo a 23:59:59.999 del día final desde la capa de servicio.
 *
 * Notas de negocio:
 * - Las Notas de Crédito se devuelven positivas; el DTO/servicio decidirá si las niega.
 * - La actividad económica se toma de la actividad principal de la EMPRESA (vía sucursal).
 *   Más adelante se puede migrar a actividad por factura sin romper firma.
 */
@Repository
public interface ReporteVentasRepository extends JpaRepository<Factura, Long> {

    /**
     * Query principal para reporte de ventas
     * Une facturas con actividad económica principal de la empresa (vía sucursal)
     * Las notas de crédito se mostrarán con valores positivos (se negarán en el DTO/servicio)
     */
    @Query(value = """
        SELECT 
            f.clave,
            f.consecutivo,
            f.tipo_documento AS tipoDocumento,
            f.fecha_emision AS fechaEmision,
            ae.codigo AS actividadEconomicaCodigo,
            ae.descripcion AS actividadEconomicaDescripcion,
            COALESCE(c.razon_social, 'CLIENTE GENERICO') AS clienteNombre,
            COALESCE(c.numero_identificacion, '000000000') AS clienteIdentificacion,
            COALESCE(c.tipo_identificacion, 'OTRO') AS clienteTipoIdentificacion,
            f.total_mercancias_gravadas AS totalMercanciasGravadas,
            f.total_mercancias_exentas AS totalMercanciasExentas,
            f.total_mercancias_exoneradas AS totalMercanciasExoneradas,
            f.total_servicios_gravados AS totalServiciosGravados,
            f.total_servicios_exentos AS totalServiciosExentos,
            f.total_servicios_exonerados AS totalServiciosExonerados,
            f.total_venta_neta AS totalVentaNeta,
            f.total_impuesto AS totalImpuesto,
            f.total_descuentos AS totalDescuentos,
            COALESCE(f.total_exonerado, 0) AS montoTotalExonerado,
            f.total_otros_cargos AS totalOtrosCargos,
            f.total_comprobante AS totalComprobante,
            f.codigo_moneda,
            f.tipo_cambio AS tipoCambio,
            f.estado
        FROM facturas f
        INNER JOIN sucursales s ON f.sucursal_id = s.id
        LEFT JOIN empresa_actividades ea ON ea.empresa_id = s.empresa_id AND ea.es_principal = true
        LEFT JOIN actividades_economicas ae ON ea.actividad_id = ae.id
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
     * Query para obtener solo documentos con exoneración
     * (mercancías exoneradas, servicios exonerados o monto total exonerado a nivel factura)
     */
    @Query(value = """
        SELECT * FROM (
            SELECT 
                f.clave,
                f.consecutivo,
                f.tipo_documento AS tipoDocumento,
                f.fecha_emision AS fechaEmision,
                ae.codigo AS actividadEconomicaCodigo,
                ae.descripcion AS actividadEconomicaDescripcion,
                COALESCE(c.razon_social, 'CLIENTE GENERICO') AS clienteNombre,
                COALESCE(c.numero_identificacion, '000000000') AS clienteIdentificacion,
                COALESCE(c.tipo_identificacion, 'OTRO') AS clienteTipoIdentificacion,
                f.total_mercancias_gravadas AS totalMercanciasGravadas,
                f.total_mercancias_exentas AS totalMercanciasExentas,
                f.total_mercancias_exoneradas AS totalMercanciasExoneradas,
                f.total_servicios_gravados AS totalServiciosGravados,
                f.total_servicios_exentos AS totalServiciosExentos,
                f.total_servicios_exonerados AS totalServiciosExonerados,
                f.total_venta_neta AS totalVentaNeta,
                f.total_impuesto AS totalImpuesto,
                f.total_descuentos AS totalDescuentos,
                COALESCE(f.total_exonerado, 0) AS montoTotalExonerado,
                f.total_otros_cargos AS totalOtrosCargos,
                f.total_comprobante AS totalComprobante,
                f.codigo_moneda,
                f.tipo_cambio AS tipoCambio,
                f.estado
            FROM facturas f
            INNER JOIN sucursales s ON f.sucursal_id = s.id
            LEFT JOIN empresa_actividades ea ON ea.empresa_id = s.empresa_id AND ea.es_principal = true
            LEFT JOIN actividades_economicas ae ON ea.actividad_id = ae.id
            LEFT JOIN clientes c ON f.cliente_id = c.id
            WHERE f.sucursal_id = :sucursalId
              AND f.fecha_emision >= :fechaDesde
              AND f.fecha_emision <= :fechaHasta
              AND f.estado IN ('ACEPTADA', 'GENERADA')
              AND f.tipo_documento IN ('FACTURA_ELECTRONICA', 'TIQUETE_ELECTRONICO', 'NOTA_CREDITO')
        ) datos
        WHERE (totalMercanciasExoneradas > 0 
            OR totalServiciosExonerados > 0 
            OR montoTotalExonerado > 0)
        ORDER BY fechaEmision ASC, consecutivo ASC
        """, nativeQuery = true)
    List<ReporteVentasLineaDTO> obtenerVentasExoneradas(
        @Param("sucursalId") Long sucursalId,
        @Param("fechaDesde") LocalDateTime fechaDesde,
        @Param("fechaHasta") LocalDateTime fechaHasta
    );
}