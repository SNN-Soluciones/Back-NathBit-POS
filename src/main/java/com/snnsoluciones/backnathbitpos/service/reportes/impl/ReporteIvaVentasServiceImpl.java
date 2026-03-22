package com.snnsoluciones.backnathbitpos.service.reportes.impl;

import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteIvaVentasLineaDTO;
import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteIvaVentasRequest;
import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteIvaVentasResponse;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.mappers.ReporteIvaVentasRowMapper;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.security.SecurityUtils;
import com.snnsoluciones.backnathbitpos.service.reportes.ReporteIvaVentasService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del reporte de IVA por tarifa en ventas.
 *
 * CORRECCIÓN: fecha_emision en facturas es VARCHAR ISO-8601, NO timestamp.
 * Se pasa como String para evitar "bad SQL grammar" al comparar con LocalDateTime.
 * Mismo patrón que ReporteVentasServiceImpl.
 *
 * created_at en factura_bitacora SÍ es TIMESTAMP real → acepta LocalDateTime.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteIvaVentasServiceImpl implements ReporteIvaVentasService {

    private final SucursalRepository       sucursalRepository;
    private final JdbcTemplate             jdbcTemplate;
    private final ReporteIvaVentasRowMapper rowMapper;

    @Override
    @Transactional(readOnly = true)
    public ReporteIvaVentasResponse generarReporte(ReporteIvaVentasRequest request) {

        request.aplicarDefaults();

        log.info("Reporte IVA — sucursal={} | emision=[{} -> {}] | estado={} | aceptacion=[{} -> {}] | tipos={}",
            request.getSucursalId(),
            request.getFechaEmisionDesde(), request.getFechaEmisionHasta(),
            request.getEstadoBitacora(),
            request.getFechaAceptacionDesde(), request.getFechaAceptacionHasta(),
            request.getTiposDocumento());

        validarSucursal(request.getSucursalId());
        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
            .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada"));

        QueryConParams qcp = buildQuery(request);

        List<ReporteIvaVentasLineaDTO> lineas = jdbcTemplate.query(
            qcp.sql(),
            rowMapper,
            qcp.params().toArray()
        );

        log.info("Reporte IVA — {} documentos para sucursal {}", lineas.size(), request.getSucursalId());

        ReporteIvaVentasResponse response = ReporteIvaVentasResponse.builder()
            .empresaNombre(sucursal.getEmpresa().getNombreRazonSocial())
            .empresaIdentificacion(sucursal.getEmpresa().getIdentificacion())
            .sucursalNombre(sucursal.getNombre())
            .fechaEmisionDesde(request.getFechaEmisionDesde())
            .fechaEmisionHasta(request.getFechaEmisionHasta())
            .estadoBitacora(request.getEstadoBitacora())
            .fechaAceptacionDesde(request.getFechaAceptacionDesde())
            .fechaAceptacionHasta(request.getFechaAceptacionHasta())
            .tiposDocumentoConsultados(request.getTiposDocumento())
            .fechaGeneracion(LocalDateTime.now())
            .generadoPor(SecurityUtils.getCurrentUserLogin())
            .lineas(lineas)
            .build();

        response.calcularTotales();
        return response;
    }

    private QueryConParams buildQuery(ReporteIvaVentasRequest req) {

        List<Object> params = new ArrayList<>();

        // factura_bitacora.created_at = TIMESTAMP real  -> LocalDateTime OK
        LocalDateTime bitacoraDesde = req.getFechaAceptacionDesde().atStartOfDay();
        LocalDateTime bitacoraHasta = req.getFechaAceptacionHasta().atTime(23, 59, 59);

        // facturas.fecha_emision = VARCHAR ISO-8601 -> pasar como String (no LocalDateTime)
        // Mismo patron de ReporteVentasServiceImpl:
        //   String fechaDesde = request.getFechaDesde().atStartOfDay().toString();
        String emisionDesde = req.getFechaEmisionDesde().atStartOfDay().toString();
        String emisionHasta  = req.getFechaEmisionHasta().atTime(23, 59, 59).toString();

        String inPlaceholders = buildInPlaceholders(req.getTiposDocumento().size());

        String sql = """
            SELECT
                f.tipo_documento,
                f.clave,
                f.consecutivo,
                COALESCE(c.razon_social, 'CLIENTE GENERICO') AS cliente_nombre,
                c.numero_identificacion                       AS cliente_identificacion,
                f.fecha_emision,

                COALESCE(SUM(CASE WHEN i.tarifa = 0
                    THEN i.monto_impuesto ELSE 0 END), 0)
                    * CASE WHEN f.tipo_documento = 'NOTA_CREDITO' THEN -1 ELSE 1 END AS iva_0,

                COALESCE(SUM(CASE WHEN i.tarifa IN (0.01, 1.00)
                    THEN i.monto_impuesto ELSE 0 END), 0)
                    * CASE WHEN f.tipo_documento = 'NOTA_CREDITO' THEN -1 ELSE 1 END AS iva_1,

                COALESCE(SUM(CASE WHEN i.tarifa IN (0.02, 2.00)
                    THEN i.monto_impuesto ELSE 0 END), 0)
                    * CASE WHEN f.tipo_documento = 'NOTA_CREDITO' THEN -1 ELSE 1 END AS iva_2,

                COALESCE(SUM(CASE WHEN i.tarifa IN (0.04, 4.00)
                    THEN i.monto_impuesto ELSE 0 END), 0)
                    * CASE WHEN f.tipo_documento = 'NOTA_CREDITO' THEN -1 ELSE 1 END AS iva_4,

                COALESCE(SUM(CASE WHEN i.tarifa IN (0.08, 8.00)
                    THEN i.monto_impuesto ELSE 0 END), 0)
                    * CASE WHEN f.tipo_documento = 'NOTA_CREDITO' THEN -1 ELSE 1 END AS iva_8,

                COALESCE(SUM(CASE WHEN i.tarifa IN (0.13, 13.00)
                    THEN i.monto_impuesto ELSE 0 END), 0)
                    * CASE WHEN f.tipo_documento = 'NOTA_CREDITO' THEN -1 ELSE 1 END AS iva_13,

                CASE WHEN f.tipo_documento = 'NOTA_CREDITO'
                    THEN -f.total_venta_neta              ELSE f.total_venta_neta              END AS total_neto,
                CASE WHEN f.tipo_documento = 'NOTA_CREDITO'
                    THEN -f.total_impuesto                ELSE f.total_impuesto                END AS total_impuestos,
                CASE WHEN f.tipo_documento = 'NOTA_CREDITO'
                    THEN -COALESCE(f.total_descuentos, 0) ELSE COALESCE(f.total_descuentos, 0) END AS descuentos,
                CASE WHEN f.tipo_documento = 'NOTA_CREDITO'
                    THEN -f.total_comprobante             ELSE f.total_comprobante             END AS total

            FROM facturas f

            JOIN (
                SELECT DISTINCT ON (factura_id) factura_id, estado, created_at
                FROM factura_bitacora
                WHERE estado      = ?
                  AND created_at >= ?
                  AND created_at <= ?
                ORDER BY factura_id, created_at DESC
            ) fb ON fb.factura_id = f.id

            LEFT JOIN clientes                 c ON f.cliente_id  = c.id
            LEFT JOIN factura_detalles         d ON d.factura_id   = f.id
            LEFT JOIN factura_detalle_impuesto i ON i.detalle_id   = d.id

            WHERE f.sucursal_id   = ?
              AND f.fecha_emision >= ?
              AND f.fecha_emision <= ?
              AND f.tipo_documento IN (""" + inPlaceholders + """
            )

            GROUP BY
                f.id, f.tipo_documento, f.clave, f.consecutivo,
                c.razon_social, c.numero_identificacion,
                f.fecha_emision,
                f.total_venta_neta, f.total_impuesto,
                f.total_descuentos, f.total_comprobante

            ORDER BY f.fecha_emision ASC
            """;

        // Orden exacto de los '?'
        params.add(req.getEstadoBitacora());     // estado = ?         (String)
        params.add(bitacoraDesde);               // created_at >= ?    (LocalDateTime)
        params.add(bitacoraHasta);               // created_at <= ?    (LocalDateTime)
        params.add(req.getSucursalId());         // sucursal_id = ?    (Long)
        params.add(emisionDesde);                // fecha_emision >= ? (String ISO)
        params.add(emisionHasta);                // fecha_emision <= ? (String ISO)
        params.addAll(req.getTiposDocumento());  // tipo_documento IN  (String...)

        return new QueryConParams(sql, params);
    }

    @Override
    public void validarSucursal(Long sucursalId) {
        if (!sucursalRepository.existsById(sucursalId)) {
            throw new EntityNotFoundException("Sucursal no encontrada: " + sucursalId);
        }
    }

    private String buildInPlaceholders(int n) {
        return "?,".repeat(n).replaceAll(",$", "");
    }

    private record QueryConParams(String sql, List<Object> params) {}
}