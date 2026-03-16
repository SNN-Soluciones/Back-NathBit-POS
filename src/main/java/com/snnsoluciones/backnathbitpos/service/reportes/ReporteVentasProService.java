package com.snnsoluciones.backnathbitpos.service.reportes;

import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteVentasProDTO;
import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteVentasProDTO.*;
import com.snnsoluciones.backnathbitpos.repository.MetricasVentasDiariasRepository;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteVentasProService {

    private final JdbcTemplate jdbcTemplate;
    private final SucursalRepository sucursalRepository;
    private final EmpresaRepository empresaRepository;

    public ReporteVentasProDTO generar(
            Long empresaId,
            Long sucursalId,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {

        boolean modoEmpresa = sucursalId == null;

        String empresaNombre = empresaRepository.findById(empresaId)
            .map(e -> e.getNombreComercial() != null ? e.getNombreComercial() : e.getNombreRazonSocial())
            .orElse("Empresa");

        String sucursalNombre = modoEmpresa ? null :
            sucursalRepository.findById(sucursalId)
                .map(s -> s.getNombre())
                .orElse("Sucursal");

        // Período anterior del mismo largo
        long dias = fechaHasta.toEpochDay() - fechaDesde.toEpochDay() + 1;
        LocalDate anteriorDesde = fechaDesde.minusDays(dias);
        LocalDate anteriorHasta = fechaDesde.minusDays(1);

        // ── 1. Ventas por día ─────────────────────────────────────────
        List<VentaDiaDTO> ventasPorDia = obtenerVentasPorDia(
            empresaId, sucursalId, fechaDesde, fechaHasta,
            anteriorDesde, anteriorHasta);

        // ── 2. Totales del período ────────────────────────────────────
        BigDecimal totalFE        = ventasPorDia.stream().map(VentaDiaDTO::getVentasFE).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalInternas  = ventasPorDia.stream().map(VentaDiaDTO::getVentasInternas).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVentas    = totalFE.add(totalInternas);
        Integer cantFE            = ventasPorDia.stream().mapToInt(d -> d.getCantidadFE() != null ? d.getCantidadFE() : 0).sum();
        Integer cantInt           = ventasPorDia.stream().mapToInt(d -> d.getCantidadInternas() != null ? d.getCantidadInternas() : 0).sum();

        BigDecimal totalAnterior  = ventasPorDia.stream()
            .map(d -> d.getTotalPeriodoAnterior() != null ? d.getTotalPeriodoAnterior() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal variacion = BigDecimal.ZERO;
        if (totalAnterior.compareTo(BigDecimal.ZERO) > 0) {
            variacion = totalVentas.subtract(totalAnterior)
                .divide(totalAnterior, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        }

        // ── 3. Impuesto y descuentos ──────────────────────────────────
        BigDecimal[] impDesc = obtenerImpuestoDescuento(empresaId, sucursalId, fechaDesde, fechaHasta);

        // ── 4. Medios de pago ─────────────────────────────────────────
        List<MedioPagoDTO> mediosPago = obtenerMediosPago(empresaId, sucursalId, fechaDesde, fechaHasta);

        // ── 5. Top productos ──────────────────────────────────────────
        List<TopProductoDTO> topProductos = obtenerTopProductos(empresaId, sucursalId, fechaDesde, fechaHasta);

        // ── 6. Por sucursal (solo modo empresa) ───────────────────────
        List<SucursalResumenDTO> porSucursal = modoEmpresa
            ? obtenerResumenPorSucursal(empresaId, fechaDesde, fechaHasta, totalVentas)
            : List.of();

        return ReporteVentasProDTO.builder()
            .empresaNombre(empresaNombre)
            .sucursalNombre(sucursalNombre)
            .fechaDesde(fechaDesde)
            .fechaHasta(fechaHasta)
            .modoEmpresa(modoEmpresa)
            .totalVentasFE(totalFE)
            .totalVentasInternas(totalInternas)
            .totalVentas(totalVentas)
            .totalImpuesto(impDesc[0])
            .totalDescuentos(impDesc[1])
            .cantidadDocumentosFE(cantFE)
            .cantidadDocumentosInternos(cantInt)
            .cantidadDocumentosTotal(cantFE + cantInt)
            .totalVentasPeriodoAnterior(totalAnterior)
            .variacionPorcentual(variacion)
            .ventasPorDia(ventasPorDia)
            .ventasPorMedioPago(mediosPago)
            .topProductos(topProductos)
            .ventasPorSucursal(porSucursal)
            .build();
    }

    // ── Ventas por día con comparativo ────────────────────────────────
    private List<VentaDiaDTO> obtenerVentasPorDia(
        Long empresaId, Long sucursalId,
        LocalDate desde, LocalDate hasta,
        LocalDate antDesde, LocalDate antHasta) {

        String sucursalFilter = sucursalId != null
            ? "AND sucursal_id = " + sucursalId
            : "AND sucursal_id IS NULL";

        String sql = """
        SELECT fecha,
               COALESCE(ventas_mh, 0)        AS fe,
               COALESCE(ventas_internas, 0)   AS internas,
               COALESCE(impuesto_total, 0)    AS impuesto,
               COALESCE(cantidad_mh, 0)       AS cant_fe,
               COALESCE(cantidad_internas, 0) AS cant_int
        FROM metricas_ventas_diarias
        WHERE empresa_id = ?
          AND fecha BETWEEN ? AND ?
          %s
        ORDER BY fecha ASC
        """.formatted(sucursalFilter);

        // Período actual
        Map<LocalDate, VentaDiaDTO> mapActual = new LinkedHashMap<>();
        jdbcTemplate.query(sql, rs -> {
            LocalDate fecha = rs.getDate("fecha").toLocalDate();
            mapActual.put(fecha, VentaDiaDTO.builder()
                .fecha(fecha)
                .diaSemana(fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es")))
                .ventasFE(rs.getBigDecimal("fe"))
                .ventasInternas(rs.getBigDecimal("internas"))
                .total(rs.getBigDecimal("fe").add(rs.getBigDecimal("internas")))
                .impuesto(rs.getBigDecimal("impuesto"))
                .cantidadFE(rs.getInt("cant_fe"))
                .cantidadInternas(rs.getInt("cant_int"))
                .totalPeriodoAnterior(BigDecimal.ZERO)
                .variacion(BigDecimal.ZERO)
                .build());
        }, empresaId, desde, hasta);

        // Período anterior
        Map<LocalDate, BigDecimal> mapAnterior = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            LocalDate fecha = rs.getDate("fecha").toLocalDate();
            BigDecimal total = rs.getBigDecimal("fe").add(rs.getBigDecimal("internas"));
            long offsetActual = desde.toEpochDay() - antDesde.toEpochDay();
            LocalDate fechaEquivalente = fecha.plusDays(offsetActual);
            mapAnterior.put(fechaEquivalente, total);
        }, empresaId, antDesde, antHasta);

        // Combinar
        mapAnterior.forEach((fecha, totalAnt) -> {
            if (mapActual.containsKey(fecha)) {
                VentaDiaDTO dia = mapActual.get(fecha);
                dia.setTotalPeriodoAnterior(totalAnt);
                if (totalAnt.compareTo(BigDecimal.ZERO) > 0) {
                    dia.setVariacion(dia.getTotal().subtract(totalAnt)
                        .divide(totalAnt, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")));
                }
            }
        });

        return new ArrayList<>(mapActual.values());
    }


    // ── Impuesto y descuentos del período ─────────────────────────────
    private BigDecimal[] obtenerImpuestoDescuento(
            Long empresaId, Long sucursalId, LocalDate desde, LocalDate hasta) {

        String sucursalFilter = sucursalId != null
            ? "AND sucursal_id = " + sucursalId
            : "AND sucursal_id IS NULL";

        String sql = """
            SELECT COALESCE(SUM(impuesto_total), 0),
                   COALESCE(SUM(descuentos_total), 0)
            FROM metricas_ventas_diarias
            WHERE empresa_id = ?
              AND fecha BETWEEN ? AND ?
              %s
            """.formatted(sucursalFilter);

        return jdbcTemplate.queryForObject(sql, (rs, rn) ->
            new BigDecimal[]{ rs.getBigDecimal(1), rs.getBigDecimal(2) },
            empresaId, desde, hasta);
    }

    // ── Medios de pago ────────────────────────────────────────────────
    private List<MedioPagoDTO> obtenerMediosPago(
        Long empresaId, Long sucursalId, LocalDate desde, LocalDate hasta) {

        String sucursalFilterFE = sucursalId != null
            ? "AND f.sucursal_id = " + sucursalId
            : "AND s.empresa_id = " + empresaId;

        String sucursalFilterFI = sucursalId != null
            ? "AND fi.sucursal_id = " + sucursalId
            : "AND s.empresa_id = " + empresaId;

        String sql = """
        SELECT mp.medio_pago,
               SUM(mp.monto) AS monto,
               COUNT(*)      AS cantidad
        FROM factura_medios_pago mp
        JOIN facturas f ON f.id = mp.factura_id
        JOIN sucursales s ON s.id = f.sucursal_id
        WHERE s.empresa_id = ?
          AND CASE
                WHEN EXTRACT(HOUR FROM f.fecha_emision::timestamp AT TIME ZONE 'America/Costa_Rica') < 4
                THEN DATE(f.fecha_emision::timestamp AT TIME ZONE 'America/Costa_Rica') - 1
                ELSE DATE(f.fecha_emision::timestamp AT TIME ZONE 'America/Costa_Rica')
              END BETWEEN ? AND ?
          AND f.estado NOT IN ('ANULADA','RECHAZADA')
          %s
        GROUP BY mp.medio_pago

        UNION ALL

        SELECT fim.tipo AS medio_pago,
               SUM(fim.monto) AS monto,
               COUNT(*)       AS cantidad
        FROM factura_interna_medios_pago fim
        JOIN factura_interna fi ON fi.id = fim.factura_interna_id
        JOIN sucursales s ON s.id = fi.sucursal_id
        WHERE s.empresa_id = ?
          AND CASE
                WHEN EXTRACT(HOUR FROM fi.fecha AT TIME ZONE 'America/Costa_Rica') < 4
                THEN DATE(fi.fecha AT TIME ZONE 'America/Costa_Rica') - 1
                ELSE DATE(fi.fecha AT TIME ZONE 'America/Costa_Rica')
              END BETWEEN ? AND ?
          AND fi.estado = 'PAGADA'
          %s
        GROUP BY fim.tipo
        """.formatted(sucursalFilterFE, sucursalFilterFI);

        Map<String, BigDecimal[]> mapa = new LinkedHashMap<>();
        jdbcTemplate.query(sql, rs -> {
            String medio = rs.getString("medio_pago");
            BigDecimal monto = rs.getBigDecimal("monto");
            int cantidad = rs.getInt("cantidad");
            mapa.merge(medio, new BigDecimal[]{ monto, new BigDecimal(cantidad) },
                (a, b) -> new BigDecimal[]{ a[0].add(b[0]), a[1].add(b[1]) });
        }, empresaId, desde, hasta, empresaId, desde, hasta);

        BigDecimal gran = mapa.values().stream()
            .map(v -> v[0])
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return mapa.entrySet().stream().map(e -> MedioPagoDTO.builder()
                .medioPago(e.getKey())
                .descripcion(traducirMedioPago(e.getKey()))
                .monto(e.getValue()[0])
                .cantidad(e.getValue()[1].intValue())
                .porcentaje(gran.compareTo(BigDecimal.ZERO) > 0
                    ? e.getValue()[0].divide(gran, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO)
                .build())
            .sorted(Comparator.comparing(MedioPagoDTO::getMonto).reversed())
            .toList();
    }

    // ── Top productos ─────────────────────────────────────────────────
    private List<TopProductoDTO> obtenerTopProductos(
        Long empresaId, Long sucursalId, LocalDate desde, LocalDate hasta) {

        String sucursalCond = sucursalId != null
            ? "AND m.sucursal_id = " + sucursalId
            : "AND s.empresa_id = " + empresaId;

        String sql = """
        SELECT p.id, p.nombre,
               STRING_AGG(DISTINCT c.nombre, ', ') AS categoria,
               COALESCE(SUM(m.cantidad_vendida), 0) AS cantidad
        FROM metricas_productos_vendidos m
        JOIN productos p ON p.id = m.producto_id
        JOIN sucursales s ON s.id = m.sucursal_id
        LEFT JOIN producto_categoria pc ON pc.producto_id = p.id
        LEFT JOIN categorias_producto c ON c.id = pc.categoria_id
        WHERE s.empresa_id = ?
          AND m.fecha BETWEEN ? AND ?
          %s
        GROUP BY p.id, p.nombre
        ORDER BY cantidad DESC
        LIMIT 10
        """.formatted(sucursalCond);

        List<TopProductoDTO> lista = jdbcTemplate.query(sql, (rs, rn) -> TopProductoDTO.builder()
            .productoId(rs.getLong("id"))
            .nombre(rs.getString("nombre"))
            .categoria(rs.getString("categoria"))
            .cantidadVendida(rs.getBigDecimal("cantidad"))
            .totalVentas(BigDecimal.ZERO) // no disponible en métricas
            .build(), empresaId, desde, hasta);

        BigDecimal gran = lista.stream()
            .map(TopProductoDTO::getCantidadVendida)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        lista.forEach(p -> p.setPorcentaje(gran.compareTo(BigDecimal.ZERO) > 0
            ? p.getCantidadVendida().divide(gran, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"))
            : BigDecimal.ZERO));

        return lista;
    }

    // ── Resumen por sucursal ──────────────────────────────────────────
    private List<SucursalResumenDTO> obtenerResumenPorSucursal(
            Long empresaId, LocalDate desde, LocalDate hasta, BigDecimal totalGeneral) {

        String sql = """
            SELECT s.id, s.nombre,
                   COALESCE(SUM(m.ventas_mh), 0)      AS fe,
                   COALESCE(SUM(m.ventas_internas), 0) AS internas,
                   COALESCE(SUM(m.cantidad_mh), 0) + COALESCE(SUM(m.cantidad_internas), 0) AS docs
            FROM metricas_ventas_diarias m
            JOIN sucursales s ON s.id = m.sucursal_id
            WHERE m.empresa_id = ?
              AND m.fecha BETWEEN ? AND ?
              AND m.sucursal_id IS NOT NULL
            GROUP BY s.id, s.nombre
            ORDER BY (fe + internas) DESC
            """;

        return jdbcTemplate.query(sql, (rs, rn) -> {
            BigDecimal fe = rs.getBigDecimal("fe");
            BigDecimal int_ = rs.getBigDecimal("internas");
            BigDecimal total = fe.add(int_);
            return SucursalResumenDTO.builder()
                .sucursalId(rs.getLong("id"))
                .sucursalNombre(rs.getString("nombre"))
                .totalFE(fe)
                .totalInternas(int_)
                .totalVentas(total)
                .cantidadDocumentos(rs.getInt("docs"))
                .porcentaje(totalGeneral.compareTo(BigDecimal.ZERO) > 0
                    ? total.divide(totalGeneral, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO)
                .build();
        }, empresaId, desde, hasta);
    }

    private String traducirMedioPago(String tipo) {
        if (tipo == null) return "Otros";
        return switch (tipo.toUpperCase()) {
            case "EFECTIVO"           -> "Efectivo";
            case "TARJETA"            -> "Tarjeta";
            case "TRANSFERENCIA"      -> "Transferencia";
            case "SINPE_MOVIL","SINPE"-> "SINPE Móvil";
            case "PLATAFORMA_DIGITAL" -> "Plataforma Digital";
            case "CHEQUE"             -> "Cheque";
            default                   -> tipo;
        };
    }
}