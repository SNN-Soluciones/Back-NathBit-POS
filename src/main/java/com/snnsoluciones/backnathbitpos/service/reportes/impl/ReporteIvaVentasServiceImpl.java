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
 * <h3>Lógica de la query</h3>
 * <ol>
 *   <li>Filtra por {@code sucursal_id} (obligatorio).</li>
 *   <li>Filtra por rango de {@code fecha_emision} del documento.</li>
 *   <li>Filtra por tipo de documento (lista variable, IN clause dinámica).</li>
 *   <li>Hace JOIN con el último registro de {@code factura_bitacora} para cada factura,
 *       filtrando por {@code estado} y rango de {@code created_at} (fecha en que
 *       Hacienda procesó el documento).</li>
 *   <li>Agrega IVA por tarifa usando CASE WHEN sobre {@code factura_detalle_impuesto}.</li>
 *   <li>Invierte el signo de todos los montos cuando el documento es NOTA_CREDITO.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteIvaVentasServiceImpl implements ReporteIvaVentasService {

    private final SucursalRepository  sucursalRepository;
    private final JdbcTemplate         jdbcTemplate;
    private final ReporteIvaVentasRowMapper rowMapper;

    // ─────────────────────────────────────────────────────────────────
    //  MÉTODO PRINCIPAL
    // ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ReporteIvaVentasResponse generarReporte(ReporteIvaVentasRequest request) {

        // 1) Aplicar defaults a todos los filtros nulos
        request.aplicarDefaults();

        log.info("Generando reporte IVA — sucursal={}, emisiónDesde={}, emisiónHasta={}, " +
                 "estado={}, aceptaciónDesde={}, aceptaciónHasta={}, tipos={}",
            request.getSucursalId(),
            request.getFechaEmisionDesde(),
            request.getFechaEmisionHasta(),
            request.getEstadoBitacora(),
            request.getFechaAceptacionDesde(),
            request.getFechaAceptacionHasta(),
            request.getTiposDocumento());

        // 2) Validar que la sucursal existe
        validarSucursal(request.getSucursalId());
        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
            .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada"));

        // 3) Construir la query dinámica y la lista de parámetros
        QueryConParams qcp = buildQuery(request);

        // 4) Ejecutar la query
        List<ReporteIvaVentasLineaDTO> lineas = jdbcTemplate.query(
            qcp.sql,
            rowMapper,
            qcp.params.toArray()
        );

        log.info("Reporte IVA — {} documentos encontrados para sucursal {}",
            lineas.size(), request.getSucursalId());

        // 5) Armar la respuesta
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

        // 6) Calcular totales y contadores
        response.calcularTotales();

        return response;
    }

    // ─────────────────────────────────────────────────────────────────
    //  BUILDER DE QUERY DINÁMICA
    // ─────────────────────────────────────────────────────────────────

    /**
     * Construye el SQL final y la lista de parámetros {@code ?} de forma dinámica
     * según los filtros activos del request.
     *
     * <p>Se usa {@link StringBuilder} en lugar de un texto fijo para poder
     * armar el IN clause de tipos de documento de manera segura (sin concatenación
     * de strings de usuario).</p>
     */
    private QueryConParams buildQuery(ReporteIvaVentasRequest req) {

        List<Object> params = new ArrayList<>();

        // ── Subquery de bitácora: último estado por factura, filtrado por estado y fechas ──
        // Nota: created_at representa cuándo Hacienda procesó el documento.
        //       Se convierte a timestamp usando atStartOfDay / atTime(23,59,59).
        LocalDateTime bitacoraDesde = req.getFechaAceptacionDesde().atStartOfDay();
        LocalDateTime bitacoraHasta = req.getFechaAceptacionHasta().atTime(23, 59, 59);

        // ── IN clause para tipos de documento ──────────────────────────────────────────────
        // Se genera un placeholder ? por cada elemento para evitar SQL injection.
        String tiposPlaceholders = buildInPlaceholders(req.getTiposDocumento().size());

        // ── Rango de fecha de emisión ──────────────────────────────────────────────────────
        LocalDateTime emisionDesde = req.getFechaEmisionDesde().atStartOfDay();
        LocalDateTime emisionHasta = req.getFechaEmisionHasta().atTime(23, 59, 59);

        // ──────────────────────────────────────────────────────────────────────────────────
        //  SQL principal
        // ──────────────────────────────────────────────────────────────────────────────────
        String sql = """
            SELECT
                f.tipo_documento,
                f.clave,
                f.consecutivo,
                COALESCE(c.razon_social, 'CLIENTE GENERICO')  AS cliente_nombre,
                c.numero_identificacion                        AS cliente_identificacion,
                f.fecha_emision,

                -- IVA 0%
                COALESCE(SUM(CASE WHEN i.tarifa = 0
                    THEN i.monto_impuesto ELSE 0 END), 0)
                    * CASE WHEN f.tipo_documento = 'NOTA_CREDITO' THEN -1 ELSE 1 END AS iva_0,

                -- IVA 1%
                COALESCE(SUM(CASE WHEN i.tarifa IN (0.01, 1.00)
                    THEN i.monto_impuesto ELSE 0 END), 0)
                    * CASE WHEN f.tipo_documento = 'NOTA_CREDITO' THEN -1 ELSE 1 END AS iva_1,

                -- IVA 2%
                COALESCE(SUM(CASE WHEN i.tarifa IN (0.02, 2.00)
                    THEN i.monto_impuesto ELSE 0 END), 0)
                    * CASE WHEN f.tipo_documento = 'NOTA_CREDITO' THEN -1 ELSE 1 END AS iva_2,

                -- IVA 4%
                COALESCE(SUM(CASE WHEN i.tarifa IN (0.04, 4.00)
                    THEN i.monto_impuesto ELSE 0 END), 0)
                    * CASE WHEN f.tipo_documento = 'NOTA_CREDITO' THEN -1 ELSE 1 END AS iva_4,

                -- IVA 8%
                COALESCE(SUM(CASE WHEN i.tarifa IN (0.08, 8.00)
                    THEN i.monto_impuesto ELSE 0 END), 0)
                    * CASE WHEN f.tipo_documento = 'NOTA_CREDITO' THEN -1 ELSE 1 END AS iva_8,

                -- IVA 13%
                COALESCE(SUM(CASE WHEN i.tarifa IN (0.13, 13.00)
                    THEN i.monto_impuesto ELSE 0 END), 0)
                    * CASE WHEN f.tipo_documento = 'NOTA_CREDITO' THEN -1 ELSE 1 END AS iva_13,

                -- Totales (con signo invertido para NC)
                CASE WHEN f.tipo_documento = 'NOTA_CREDITO'
                    THEN -f.total_venta_neta              ELSE f.total_venta_neta              END AS total_neto,
                CASE WHEN f.tipo_documento = 'NOTA_CREDITO'
                    THEN -f.total_impuesto                ELSE f.total_impuesto                END AS total_impuestos,
                CASE WHEN f.tipo_documento = 'NOTA_CREDITO'
                    THEN -COALESCE(f.total_descuentos, 0) ELSE COALESCE(f.total_descuentos, 0) END AS descuentos,
                CASE WHEN f.tipo_documento = 'NOTA_CREDITO'
                    THEN -f.total_comprobante             ELSE f.total_comprobante             END AS total

            FROM facturas f

            -- Solo el último registro de bitácora por factura, filtrado por estado y fechas
            JOIN (
                SELECT DISTINCT ON (factura_id) factura_id, estado, created_at
                FROM factura_bitacora
                WHERE estado      = ?
                  AND created_at >= ?
                  AND created_at  < ?
                ORDER BY factura_id, created_at DESC
            ) fb ON fb.factura_id = f.id

            LEFT JOIN clientes               c ON f.cliente_id  = c.id
            LEFT JOIN factura_detalles       d ON d.factura_id   = f.id
            LEFT JOIN factura_detalle_impuesto i ON i.detalle_id = d.id

            WHERE f.sucursal_id   = ?
              AND f.fecha_emision >= ?
              AND f.fecha_emision <= ?
              AND f.tipo_documento IN (""" + tiposPlaceholders + """
            )

            GROUP BY
                f.id, f.tipo_documento, f.clave, f.consecutivo,
                c.razon_social, c.numero_identificacion,
                f.fecha_emision,
                f.total_venta_neta, f.total_impuesto,
                f.total_descuentos, f.total_comprobante

            ORDER BY f.fecha_emision ASC
            """;

        // ── Registrar parámetros en el orden exacto de los ? ──────────────────────────────

        // Subquery bitácora
        params.add(req.getEstadoBitacora());          // estado = ?
        params.add(bitacoraDesde);                    // created_at >= ?
        params.add(bitacoraHasta);                    // created_at < ?  (día siguiente 00:00)

        // WHERE principal
        params.add(req.getSucursalId());              // sucursal_id = ?
        params.add(emisionDesde);                     // fecha_emision >= ?
        params.add(emisionHasta);                     // fecha_emision <= ?

        // IN clause tipos de documento
        params.addAll(req.getTiposDocumento());        // tipo_documento IN (?, ?, ...)

        return new QueryConParams(sql, params);
    }

    // ─────────────────────────────────────────────────────────────────
    //  VALIDACIÓN
    // ─────────────────────────────────────────────────────────────────

    @Override
    public void validarSucursal(Long sucursalId) {
        if (!sucursalRepository.existsById(sucursalId)) {
            throw new EntityNotFoundException("Sucursal no encontrada: " + sucursalId);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  HELPERS INTERNOS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Genera una cadena de {@code n} placeholders separados por coma.
     * Ejemplo: {@code buildInPlaceholders(3)} → {@code "?, ?, ?"}
     */
    private String buildInPlaceholders(int n) {
        return "?,".repeat(n).replaceAll(",$", "");
    }

    /**
     * Contenedor inmutable para SQL + parámetros posicionales.
     */
    private record QueryConParams(String sql, List<Object> params) {}
}