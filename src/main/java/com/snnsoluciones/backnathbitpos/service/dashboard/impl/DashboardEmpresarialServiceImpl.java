package com.snnsoluciones.backnathbitpos.service.dashboard.impl;
 
import com.snnsoluciones.backnathbitpos.dto.dashboard.empresarial.*;
import com.snnsoluciones.backnathbitpos.entity.global.SuperAdminTenant;
import com.snnsoluciones.backnathbitpos.entity.global.Tenant;
import com.snnsoluciones.backnathbitpos.repository.global.SuperAdminTenantRepository;
import com.snnsoluciones.backnathbitpos.service.dashboard.DashboardEmpresarialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
 
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
 
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardEmpresarialServiceImpl implements DashboardEmpresarialService {
 
    private final SuperAdminTenantRepository superAdminTenantRepository;
    private final JdbcTemplate jdbcTemplate;
 
    // ── Obtener tenants del usuario ────────────────────────────────────────
 
    /**
     * Retorna los tenants activos asignados al usuario global.
     * Si empresaId viene, filtra solo ese tenant.
     */
    private List<Tenant> obtenerTenants(Long usuarioGlobalId, Long empresaIdFiltro) {
        List<SuperAdminTenant> relaciones = superAdminTenantRepository.findByUsuarioIdAndActivoTrue(usuarioGlobalId);

        List<Tenant> tenants = relaciones.stream()
            .map(SuperAdminTenant::getTenant)
            .filter(t -> Boolean.TRUE.equals(t.getActivo()))
            .filter(t -> empresaIdFiltro == null || t.getEmpresaLegacyId().equals(empresaIdFiltro))
            .toList();

        log.debug("Tenants para usuario {}: {}", usuarioGlobalId, tenants.stream().map(Tenant::getCodigo).toList());
        return tenants;
    }
 
    // ── Período anterior (mismo rango de días) ─────────────────────────────
    private LocalDate[] periodoAnterior(LocalDate desde, LocalDate hasta) {
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        return new LocalDate[]{ desde.minusDays(dias), hasta.minusDays(dias) };
    }

    //
    // 0. Resumen empresa
    //
    @Override
    public EmpresasResumenResponse empresasResumen(Long uid) {
        List<Tenant> tenants = obtenerTenants(uid, null);
        LocalDate hoy = LocalDate.now();
        LocalDate ayer = hoy.minusDays(1);
        List<EmpresasResumenResponse.EmpresaCard> empresas = new ArrayList<>();

        for (Tenant tenant : tenants) {
            String schema = tenant.getSchemaName();

            BigDecimal ventasHoy  = ventasTotalesSchema(schema, hoy, hoy);
            BigDecimal ventasAyer = ventasTotalesSchema(schema, ayer, ayer);
            long facturasHoy      = contarFacturasSchema(schema, hoy, hoy);

            // Cajas abiertas
            long cajas = 0;
            try {
                Long c = jdbcTemplate.queryForObject(
                    String.format("SELECT COUNT(*) FROM %s.v2_sesion_caja WHERE estado = 'ABIERTA'", schema), Long.class);
                cajas = c != null ? c : 0;
            } catch (Exception e) { log.warn("Cajas {}: {}", schema, e.getMessage()); }

            // Sucursales totales
            long sucursales = 0;
            try {
                Long s = jdbcTemplate.queryForObject(
                    String.format("SELECT COUNT(*) FROM %s.sucursales WHERE activa = true", schema), Long.class);
                sucursales = s != null ? s : 0;
            } catch (Exception e) { log.warn("Sucursales {}: {}", schema, e.getMessage()); }

            // Última venta (FE o FI, la más reciente)
            LocalDateTime ultimaVenta = null;
            try {
                String sqlUltima = String.format("""
                SELECT MAX(fecha) FROM (
                    SELECT MAX(fecha_emision::timestamp) AS fecha FROM %s.facturas
                    WHERE estado NOT IN ('ANULADA','ERROR')
                    UNION ALL
                    SELECT MAX(fecha::timestamp) FROM %s.factura_interna
                    WHERE estado != 'ANULADA'
                ) v
                """, schema, schema);
                ultimaVenta = jdbcTemplate.queryForObject(sqlUltima, LocalDateTime.class);
            } catch (Exception e) { log.debug("Sin ultima venta {}", schema); }

            BigDecimal porcentaje = BigDecimal.ZERO;
            String tendencia = "stable";
            if (ventasAyer.compareTo(BigDecimal.ZERO) > 0) {
                porcentaje = ventasHoy.subtract(ventasAyer)
                    .divide(ventasAyer, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
                tendencia = porcentaje.compareTo(BigDecimal.ZERO) > 0 ? "up"
                    : porcentaje.compareTo(BigDecimal.ZERO) < 0 ? "down" : "stable";
            }

            empresas.add(EmpresasResumenResponse.EmpresaCard.builder()
                .empresaId(tenant.getEmpresaLegacyId())
                .empresaNombre(tenant.getNombre())
                .activa(true)
                .ventasHoy(ventasHoy)
                .ventasAyer(ventasAyer)
                .porcentajeVsAyer(porcentaje)
                .tendencia(tendencia)
                .cajasAbiertas(cajas)
                .facturasHoy(facturasHoy)
                .sucursales(sucursales)
                .ultimaVenta(ultimaVenta)
                .build());
        }

        return EmpresasResumenResponse.builder().empresas(empresas).build();
    }
 
    // ══════════════════════════════════════════════════════════════════════
    // 1. RESUMEN GENERAL
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public ResumenEmpresarialResponse resumen(Long usuarioGlobalId, LocalDate desde, LocalDate hasta, Long empresaId) {
        List<Tenant> tenants = obtenerTenants(usuarioGlobalId, empresaId);
        LocalDate[] anterior = periodoAnterior(desde, hasta);

        BigDecimal ventasActual   = BigDecimal.ZERO;
        BigDecimal ventasAnterior = BigDecimal.ZERO;
        long totalFacturas        = 0;
        long totalEmpresas        = tenants.size();
        long empresasActivas      = 0;
        long totalSucursales      = 0;
        long totalUsuarios        = 0;

        for (Tenant tenant : tenants) {
            String schema = tenant.getSchemaName();

            // Ventas período actual (FE + internas)
            BigDecimal veActual = ventasTotalesSchema(schema, desde, hasta);
            // Ventas período anterior
            BigDecimal veAnterior = ventasTotalesSchema(schema, anterior[0], anterior[1]);
            // Facturas actuales
            long facturas = contarFacturasSchema(schema, desde, hasta);

            ventasActual   = ventasActual.add(veActual);
            ventasAnterior = ventasAnterior.add(veAnterior);
            totalFacturas  += facturas;

            if (veActual.compareTo(BigDecimal.ZERO) > 0) empresasActivas++;

            // Sucursales y usuarios del tenant
            totalSucursales += contarRegistros(schema, "sucursales");
            totalUsuarios   += contarRegistros(schema, "usuarios");
        }

        // Calcular cambio porcentual
        BigDecimal porcentajeCambio = BigDecimal.ZERO;
        String tendencia = "stable";
        if (ventasAnterior.compareTo(BigDecimal.ZERO) > 0) {
            porcentajeCambio = ventasActual.subtract(ventasAnterior)
                .divide(ventasAnterior, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
            tendencia = porcentajeCambio.compareTo(BigDecimal.ZERO) > 0 ? "up"
                : porcentajeCambio.compareTo(BigDecimal.ZERO) < 0 ? "down"
                    : "stable";
        }

        BigDecimal ticketPromedio = totalFacturas > 0
            ? ventasActual.divide(BigDecimal.valueOf(totalFacturas), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return ResumenEmpresarialResponse.builder()
            .ventasActual(ventasActual)
            .ventasAnterior(ventasAnterior)
            .porcentajeCambio(porcentajeCambio)
            .tendencia(tendencia)
            .ticketPromedio(ticketPromedio)
            .totalFacturas(totalFacturas)
            .totalEmpresas(totalEmpresas)
            .empresasActivas(empresasActivas)
            .totalSucursales(totalSucursales)
            .totalUsuarios(totalUsuarios)
            .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 2. SERIE TEMPORAL
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public VentasSerieResponse ventasSerie(Long usuarioGlobalId, LocalDate desde, LocalDate hasta,
        Long empresaId, Long sucursalId, String agruparPor) {
        List<Tenant> tenants = obtenerTenants(usuarioGlobalId, empresaId);

        // Determinar agrupación automática
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        String agrupacion = "auto".equals(agruparPor)
            ? (dias <= 31 ? "dia" : dias <= 90 ? "semana" : "mes")
            : agruparPor;

        // Generar todas las fechas del rango (para rellenar con 0)
        List<String> todasLasFechas = generarFechas(desde, hasta, agrupacion);

        List<VentasSerieResponse.SeriePorEmpresa> series = new ArrayList<>();

        for (Tenant tenant : tenants) {
            String schema = tenant.getSchemaName();

            // Query con agrupación dinámica — FE usa fecha_emision, FI usa fecha (ambas VARCHAR → castear)
            String truncFnFE = switch (agrupacion) {
                case "semana" -> "DATE_TRUNC('week', fecha_emision::timestamp)::date";
                case "mes"    -> "DATE_TRUNC('month', fecha_emision::timestamp)::date";
                default       -> "DATE(fecha_emision::timestamp)";
            };
            String truncFnFI = switch (agrupacion) {
                case "semana" -> "DATE_TRUNC('week', fecha::timestamp)::date";
                case "mes"    -> "DATE_TRUNC('month', fecha::timestamp)::date";
                default       -> "DATE(fecha::timestamp)";
            };

            // WHERE por sucursal si viene
            String whereSucursal = sucursalId != null
                ? " AND sucursal_id = " + sucursalId
                : "";

            // Ventas FE + Internas agrupadas por fecha
            String sql = String.format("""
                SELECT fecha::text, SUM(total) AS total
                FROM (
                    SELECT %s AS fecha, total_comprobante AS total
                    FROM %s.facturas
                    WHERE fecha_emision::timestamp BETWEEN ? AND ?
                      AND estado NOT IN ('ANULADA', 'ERROR')
                      %s
                    UNION ALL
                    SELECT %s AS fecha, total AS total
                    FROM %s.factura_interna
                    WHERE fecha::timestamp BETWEEN ? AND ?
                      %s
                ) ventas
                GROUP BY fecha
                ORDER BY fecha
                """, truncFnFE, schema, whereSucursal, truncFnFI, schema, whereSucursal);

            LocalDateTime desdeDT = desde.atStartOfDay();
            LocalDateTime hastaDT = hasta.atTime(23, 59, 59);

            Map<String, BigDecimal> puntosMap = new LinkedHashMap<>();
            todasLasFechas.forEach(f -> puntosMap.put(f, BigDecimal.ZERO));

            jdbcTemplate.query(sql,
                rs -> {
                    puntosMap.put(rs.getString("fecha"), rs.getBigDecimal("total"));
                },
                desdeDT, hastaDT, desdeDT, hastaDT
            );

            List<VentasSerieResponse.PuntoFecha> puntos = puntosMap.entrySet().stream()
                .map(e -> VentasSerieResponse.PuntoFecha.builder()
                    .fecha(e.getKey())
                    .total(e.getValue())
                    .build())
                .toList();

            series.add(VentasSerieResponse.SeriePorEmpresa.builder()
                .empresaId(tenant.getEmpresaLegacyId())
                .empresaNombre(tenant.getNombre())
                .puntos(puntos)
                .build());
        }

        return VentasSerieResponse.builder()
            .series(series)
            .agrupadoPor(agrupacion)
            .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 3. VENTAS POR EMPRESA
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public VentasPorEmpresaResponse ventasPorEmpresa(Long usuarioGlobalId, LocalDate desde, LocalDate hasta) {
        List<Tenant> tenants = obtenerTenants(usuarioGlobalId, null);
        LocalDate[] anterior = periodoAnterior(desde, hasta);

        List<VentasPorEmpresaResponse.EmpresaVentas> empresas = new ArrayList<>();

        for (Tenant tenant : tenants) {
            String schema = tenant.getSchemaName();

            BigDecimal totalActual   = ventasTotalesSchema(schema, desde, hasta);
            BigDecimal totalAnterior = ventasTotalesSchema(schema, anterior[0], anterior[1]);
            long facturas            = contarFacturasSchema(schema, desde, hasta);

            BigDecimal porcentajeCambio = BigDecimal.ZERO;
            String tendencia = "stable";
            if (totalAnterior.compareTo(BigDecimal.ZERO) > 0) {
                porcentajeCambio = totalActual.subtract(totalAnterior)
                    .divide(totalAnterior, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
                tendencia = porcentajeCambio.compareTo(BigDecimal.ZERO) > 0 ? "up"
                    : porcentajeCambio.compareTo(BigDecimal.ZERO) < 0 ? "down"
                        : "stable";
            }

            empresas.add(VentasPorEmpresaResponse.EmpresaVentas.builder()
                .empresaId(tenant.getEmpresaLegacyId())
                .empresaNombre(tenant.getNombre())
                .totalActual(totalActual)
                .totalAnterior(totalAnterior)
                .porcentajeCambio(porcentajeCambio)
                .tendencia(tendencia)
                .totalFacturas(facturas)
                .build());
        }

        // Ordenar por total descendente
        empresas.sort((a, b) -> b.getTotalActual().compareTo(a.getTotalActual()));

        return VentasPorEmpresaResponse.builder().empresas(empresas).build();
    }

    @Override
    public TipoPagoResponse tipoPago(Long uid, LocalDate desde, LocalDate hasta, Long empresaId, Long sucursalId) {
        List<Tenant> tenants = obtenerTenants(uid, empresaId);

        Map<String, BigDecimal> totalesPorTipo = new LinkedHashMap<>();
        // Inicializar tipos conocidos
        List.of("EFECTIVO", "TARJETA", "SINPE_MOVIL", "TRANSFERENCIA", "OTRO")
            .forEach(t -> totalesPorTipo.put(t, BigDecimal.ZERO));

        LocalDateTime desdeDT = desde.atStartOfDay();
        LocalDateTime hastaDT = hasta.atTime(23, 59, 59);

        for (Tenant tenant : tenants) {
            String schema = tenant.getSchemaName();
            String whereSucursal = sucursalId != null ? " AND sucursal_id = " + sucursalId : "";

            String sql = String.format("""
                SELECT medio_norm, SUM(monto) AS total FROM (
                    -- FE
                    SELECT
                        CASE
                            WHEN mp.medio_pago IN ('SINPE','SINPE_MOVIL') THEN 'SINPE_MOVIL'
                            WHEN mp.medio_pago IN ('DEPOSITO','TRANSFERENCIA_BANCARIA','TRANSFERENCIA') THEN 'TRANSFERENCIA'
                            WHEN mp.medio_pago = 'EFECTIVO' THEN 'EFECTIVO'
                            WHEN mp.medio_pago = 'TARJETA' THEN 'TARJETA'
                            ELSE 'OTRO'
                        END AS medio_norm,
                        mp.monto
                    FROM %s.factura_medios_pago mp
                    JOIN %s.facturas f ON f.id = mp.factura_id
                    WHERE f.fecha_emision::timestamp BETWEEN ? AND ?
                      AND f.estado NOT IN ('ANULADA','ERROR')
                      %s
                    UNION ALL
                    -- FI
                    SELECT
                        CASE
                            WHEN UPPER(mp.tipo) IN ('SINPE','SINPE_MOVIL') THEN 'SINPE_MOVIL'
                            WHEN UPPER(mp.tipo) IN ('DEPOSITO','TRANSFERENCIA_BANCARIA','TRANSFERENCIA') THEN 'TRANSFERENCIA'
                            WHEN UPPER(mp.tipo) = 'EFECTIVO' THEN 'EFECTIVO'
                            WHEN UPPER(mp.tipo) = 'TARJETA' THEN 'TARJETA'
                            ELSE 'OTRO'
                        END AS medio_norm,
                        mp.monto
                    FROM %s.factura_interna_medios_pago mp
                    JOIN %s.factura_interna fi ON fi.id = mp.factura_interna_id
                    WHERE fi.fecha::timestamp BETWEEN ? AND ?
                      AND fi.estado != 'ANULADA'
                      %s
                ) t GROUP BY medio_norm
                """, schema, schema, whereSucursal, schema, schema, whereSucursal);

            jdbcTemplate.query(sql,
                rs -> {
                    String tipo = rs.getString("medio_norm");
                    BigDecimal total = rs.getBigDecimal("total");
                    totalesPorTipo.merge(tipo, total, BigDecimal::add);
                },
                desdeDT, hastaDT, desdeDT, hastaDT
            );
        }

        BigDecimal totalGeneral = totalesPorTipo.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TipoPagoResponse.TipoPago> tipos = totalesPorTipo.entrySet().stream()
            .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) > 0)
            .map(e -> {
                BigDecimal porcentaje = totalGeneral.compareTo(BigDecimal.ZERO) > 0
                    ? e.getValue().divide(totalGeneral, 4, RoundingMode.HALF_UP)
                      .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
                return TipoPagoResponse.TipoPago.builder()
                    .tipo(e.getKey())
                    .total(e.getValue())
                    .porcentaje(porcentaje)
                    .build();
            })
            .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
            .toList();

        return TipoPagoResponse.builder().tipos(tipos).totalGeneral(totalGeneral).build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 5. TOP SUCURSALES
    // ══════════════════════════════════════════════════════════════════════
    @Override
    public TopSucursalesResponse topSucursales(Long uid, LocalDate desde, LocalDate hasta, int limit) {
        List<Tenant> tenants = obtenerTenants(uid, null);
        LocalDateTime desdeDT = desde.atStartOfDay();
        LocalDateTime hastaDT = hasta.atTime(23, 59, 59);

        record SucursalData(Long sucursalId, String nombre, String empresa, BigDecimal total, long facturas) {}
        List<SucursalData> todas = new ArrayList<>();
        BigDecimal totalGeneral = BigDecimal.ZERO;

        for (Tenant tenant : tenants) {
            String schema = tenant.getSchemaName();
            String sql = String.format("""
                SELECT s.id, s.nombre, SUM(v.total) AS total, COUNT(v.id) AS facturas
                FROM %s.sucursales s
                LEFT JOIN (
                    SELECT sucursal_id, id, total_comprobante AS total FROM %s.facturas
                    WHERE fecha_emision::timestamp BETWEEN ? AND ? AND estado NOT IN ('ANULADA','ERROR')
                    UNION ALL
                    SELECT sucursal_id, id, total FROM %s.factura_interna
                    WHERE fecha::timestamp BETWEEN ? AND ? AND estado != 'ANULADA'
                ) v ON v.sucursal_id = s.id
                GROUP BY s.id, s.nombre
                HAVING SUM(v.total) > 0
                """, schema, schema, schema);

            jdbcTemplate.query(sql,
                rs -> {
                    todas.add(new SucursalData(
                        rs.getLong("id"),
                        rs.getString("nombre"),
                        tenant.getNombre(),
                        rs.getBigDecimal("total"),
                        rs.getLong("facturas")
                    ));
                },
                desdeDT, hastaDT, desdeDT, hastaDT
            );
        }

        // Total general para porcentaje
        BigDecimal tg = todas.stream().map(SucursalData::total)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TopSucursalesResponse.SucursalTop> sucursales = todas.stream()
            .sorted((a, b) -> b.total().compareTo(a.total()))
            .limit(limit)
            .map(s -> {
                BigDecimal porcentaje = tg.compareTo(BigDecimal.ZERO) > 0
                    ? s.total().divide(tg, 4, RoundingMode.HALF_UP)
                      .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
                BigDecimal ticket = s.facturas() > 0
                    ? s.total().divide(BigDecimal.valueOf(s.facturas()), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
                return TopSucursalesResponse.SucursalTop.builder()
                    .sucursalId(s.sucursalId())
                    .sucursalNombre(s.nombre())
                    .empresaNombre(s.empresa())
                    .total(s.total())
                    .porcentaje(porcentaje)
                    .totalFacturas(s.facturas())
                    .ticketPromedio(ticket)
                    .build();
            })
            .toList();

        return TopSucursalesResponse.builder().sucursales(sucursales).build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 6. TIEMPO REAL
    // ══════════════════════════════════════════════════════════════════════
    @Override
    public TiempoRealResponse tiempoReal(Long uid) {
        List<Tenant> tenants = obtenerTenants(uid, null);

        LocalDate hoy = LocalDate.now();
        LocalDate ayer = hoy.minusDays(1);
        LocalDateTime hoySt   = hoy.atStartOfDay();
        LocalDateTime hoyEnd  = hoy.atTime(23, 59, 59);
        LocalDateTime ayerSt  = ayer.atStartOfDay();
        LocalDateTime ayerEnd = ayer.atTime(LocalDateTime.now().toLocalTime()); // mismo horario

        BigDecimal ventasHoy  = BigDecimal.ZERO;
        BigDecimal ventasAyer = BigDecimal.ZERO;
        long facturasHoy      = 0;
        long cajasAbiertas    = 0;

        for (Tenant tenant : tenants) {
            String schema = tenant.getSchemaName();

            ventasHoy  = ventasHoy.add(ventasTotalesSchema(schema, hoy, hoy));
            ventasAyer = ventasAyer.add(ventasTotalesSchema(schema, ayer, ayer));
            facturasHoy += contarFacturasSchema(schema, hoy, hoy);

            // Cajas abiertas (v2_sesion_caja con estado ABIERTA)
            try {
                Long cajas = jdbcTemplate.queryForObject(
                    String.format("SELECT COUNT(*) FROM %s.v2_sesion_caja WHERE estado = 'ABIERTA'", schema),
                    Long.class
                );
                cajasAbiertas += cajas != null ? cajas : 0;
            } catch (Exception e) {
                log.warn("Error contando cajas en {}: {}", schema, e.getMessage());
            }
        }

        return TiempoRealResponse.builder()
            .ventasHoy(ventasHoy)
            .ventasAyer(ventasAyer)
            .facturasHoy(facturasHoy)
            .cajasAbiertas(cajasAbiertas)
            .ultimaActualizacion(LocalDateTime.now())
            .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 7. TOP PRODUCTOS
    // FE: factura_detalles.descripcion + cantidad + monto_total_linea
    // FI: factura_interna_detalle.nombre + cantidad + total
    // ══════════════════════════════════════════════════════════════════════
    @Override
    public TopProductosResponse topProductos(Long uid, LocalDate desde, LocalDate hasta, Long empresaId, int limit) {
        List<Tenant> tenants = obtenerTenants(uid, empresaId);
        LocalDateTime desdeDT = desde.atStartOfDay();
        LocalDateTime hastaDT = hasta.atTime(23, 59, 59);

        Map<String, BigDecimal[]> acumulado = new LinkedHashMap<>(); // nombre → [cantidad, monto]
        BigDecimal totalVentas = BigDecimal.ZERO;

        for (Tenant tenant : tenants) {
            String schema = tenant.getSchemaName();

            String sql = String.format("""
                SELECT nombre, SUM(cantidad) AS cantidad, SUM(monto) AS monto FROM (
                    SELECT
                        COALESCE(fd.descripcion_personalizada, fd.detalle) AS nombre,
                        fd.cantidad,
                        fd.monto_total_linea AS monto
                    FROM %s.factura_detalles fd
                    JOIN %s.facturas f ON f.id = fd.factura_id
                    WHERE f.fecha_emision::timestamp BETWEEN ? AND ?
                      AND f.estado NOT IN ('ANULADA','ERROR')
                    UNION ALL
                    SELECT
                        fid.nombre_producto,
                        fid.cantidad,
                        fid.total AS monto
                    FROM %s.factura_interna_detalle fid
                    JOIN %s.factura_interna fi ON fi.id = fid.factura_interna_id
                    WHERE fi.fecha::timestamp BETWEEN ? AND ?
                      AND fi.estado != 'ANULADA'
                ) p GROUP BY nombre
                """, schema, schema, schema, schema);

            jdbcTemplate.query(sql,
                rs -> {
                    String nombre = rs.getString("nombre");
                    BigDecimal[] datos = acumulado.computeIfAbsent(nombre,
                        k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                    datos[0] = datos[0].add(rs.getBigDecimal("cantidad"));
                    datos[1] = datos[1].add(rs.getBigDecimal("monto"));
                },
                desdeDT, hastaDT, desdeDT, hastaDT
            );

            totalVentas = totalVentas.add(ventasTotalesSchema(schema, desde, hasta));
        }

        final BigDecimal tg = totalVentas;

        List<TopProductosResponse.Producto> productos = acumulado.entrySet().stream()
            .sorted((a, b) -> b.getValue()[1].compareTo(a.getValue()[1]))
            .limit(limit)
            .map(e -> {
                BigDecimal porcentaje = tg.compareTo(BigDecimal.ZERO) > 0
                    ? e.getValue()[1].divide(tg, 4, RoundingMode.HALF_UP)
                      .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
                return TopProductosResponse.Producto.builder()
                    .productoNombre(e.getKey())
                    .cantidadVendida(e.getValue()[0].longValue())
                    .totalVentas(e.getValue()[1])
                    .porcentaje(porcentaje)
                    .build();
            })
            .toList();

        return TopProductosResponse.builder().productos(productos).build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 8. ALERTAS
    // ══════════════════════════════════════════════════════════════════════
    @Override
    public AlertasResponse alertas(Long uid) {
        List<Tenant> tenants = obtenerTenants(uid, null);
        List<AlertasResponse.Alerta> alertas = new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now();

        for (Tenant tenant : tenants) {
            String schema = tenant.getSchemaName();

            // ── CAJA_ABIERTA_MUCHO (>12h) ──────────────────────────────
            try {
                String sqlCajas = String.format("""
                    SELECT sc.id, s.nombre AS sucursal, sc.fecha_apertura
                    FROM %s.v2_sesion_caja sc
                    JOIN %s.sucursales s ON s.id = sc.sucursal_id
                    WHERE sc.estado = 'ABIERTA'
                      AND sc.fecha_apertura < ?
                    """, schema, schema);

                jdbcTemplate.query(sqlCajas,
                    rs -> {
                        alertas.add(AlertasResponse.Alerta.builder()
                            .tipo("CAJA_ABIERTA_MUCHO")
                            .severidad("warning")
                            .empresa(tenant.getNombre())
                            .sucursal(rs.getString("sucursal"))
                            .mensaje("Caja abierta hace más de 12 horas")
                            .desde(rs.getTimestamp("fecha_apertura").toLocalDateTime())
                            .build());
                    },
                    ahora.minusHours(12)
                );
            } catch (Exception e) {
                log.warn("Error alertas cajas {}: {}", schema, e.getMessage());
            }

            // ── ERROR_FE (facturas con error en últimas 24h) ───────────
            try {
                String sqlFe = String.format("""
                    SELECT s.nombre AS sucursal, COUNT(*) AS cantidad, MIN(f.fecha_emision) AS desde
                    FROM %s.facturas f
                    JOIN %s.sucursales s ON s.id = f.sucursal_id
                    WHERE f.estado = 'ERROR'
                      AND f.fecha_emision::timestamp >= ?
                    GROUP BY s.id, s.nombre
                    HAVING COUNT(*) > 0
                    """, schema, schema);

                jdbcTemplate.query(sqlFe,
                    rs -> {
                        alertas.add(AlertasResponse.Alerta.builder()
                            .tipo("ERROR_FE")
                            .severidad("error")
                            .empresa(tenant.getNombre())
                            .sucursal(rs.getString("sucursal"))
                            .mensaje(rs.getLong("cantidad") + " factura(s) electrónica(s) con error de envío")
                            .desde(rs.getTimestamp("desde").toLocalDateTime())
                            .build());
                    },
                    ahora.minusHours(24)
                );
            } catch (Exception e) {
                log.warn("Error alertas FE {}: {}", schema, e.getMessage());
            }

            // ── SIN_VENTAS (sucursales sin ventas en 3h en horario comercial) ──
            try {
                int hora = ahora.getHour();
                if (hora >= 7 && hora <= 21) { // horario comercial
                    String sqlSinVentas = String.format("""
                        SELECT s.id, s.nombre
                        FROM %s.sucursales s
                        WHERE s.activo = true
                          AND NOT EXISTS (
                              SELECT 1 FROM %s.facturas f
                              WHERE f.sucursal_id = s.id
                                AND f.fecha_emision::timestamp >= ?
                                AND f.estado NOT IN ('ANULADA','ERROR')
                          )
                          AND NOT EXISTS (
                              SELECT 1 FROM %s.factura_interna fi
                              WHERE fi.sucursal_id = s.id
                                AND fi.fecha::timestamp >= ?
                                AND fi.estado != 'ANULADA'
                          )
                        """, schema, schema, schema);

                    LocalDateTime hace3h = ahora.minusHours(3);
                    jdbcTemplate.query(sqlSinVentas,
                        rs -> {
                            alertas.add(AlertasResponse.Alerta.builder()
                                .tipo("SIN_VENTAS")
                                .severidad("warning")
                                .empresa(tenant.getNombre())
                                .sucursal(rs.getString("nombre"))
                                .mensaje("Sin ventas en las últimas 3 horas")
                                .desde(hace3h)
                                .build());
                        },
                        hace3h, hace3h
                    );
                }
            } catch (Exception e) {
                log.warn("Error alertas sin ventas {}: {}", schema, e.getMessage());
            }
        }

        return AlertasResponse.builder()
            .alertas(alertas)
            .totalAlertas(alertas.size())
            .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 9. HORAS PICO
    // ══════════════════════════════════════════════════════════════════════
    @Override
    public HorasPicoResponse horasPico(Long uid, LocalDate desde, LocalDate hasta, Long empresaId, Long sucursalId) {
        List<Tenant> tenants = obtenerTenants(uid, empresaId);
        LocalDateTime desdeDT = desde.atStartOfDay();
        LocalDateTime hastaDT = hasta.atTime(23, 59, 59);

        // Acumular por hora (0-23)
        BigDecimal[] totalesPorHora   = new BigDecimal[24];
        long[]       facturasPorHora  = new long[24];
        for (int i = 0; i < 24; i++) { totalesPorHora[i] = BigDecimal.ZERO; facturasPorHora[i] = 0; }

        for (Tenant tenant : tenants) {
            String schema = tenant.getSchemaName();
            String whereSucursal = sucursalId != null ? " AND sucursal_id = " + sucursalId : "";

            String sql = String.format("""
                SELECT hora, SUM(total) AS total, COUNT(*) AS facturas FROM (
                    SELECT EXTRACT(HOUR FROM fecha_emision::timestamp)::int AS hora,
                           total_comprobante AS total
                    FROM %s.facturas
                    WHERE fecha_emision::timestamp BETWEEN ? AND ?
                      AND estado NOT IN ('ANULADA','ERROR')
                      %s
                    UNION ALL
                    SELECT EXTRACT(HOUR FROM fecha::timestamp)::int AS hora,
                           total
                    FROM %s.factura_interna
                    WHERE fecha::timestamp BETWEEN ? AND ?
                      AND estado != 'ANULADA'
                      %s
                ) h GROUP BY hora ORDER BY hora
                """, schema, whereSucursal, schema, whereSucursal);

            jdbcTemplate.query(sql,
                rs -> {
                    int hora = rs.getInt("hora");
                    totalesPorHora[hora]  = totalesPorHora[hora].add(rs.getBigDecimal("total"));
                    facturasPorHora[hora] += rs.getLong("facturas");
                },
                desdeDT, hastaDT, desdeDT, hastaDT
            );
        }

        // Encontrar hora pico
        int horaPico = 0;
        for (int i = 1; i < 24; i++) {
            if (totalesPorHora[i].compareTo(totalesPorHora[horaPico]) > 0) horaPico = i;
        }

        long totalFacturas = 0;
        for (long f : facturasPorHora) totalFacturas += f;

        List<HorasPicoResponse.HoraData> horas = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            horas.add(HorasPicoResponse.HoraData.builder()
                .hora(i)
                .total(totalesPorHora[i])
                .facturas(facturasPorHora[i])
                .build());
        }

        return HorasPicoResponse.builder()
            .horas(horas)
            .horaPico(horaPico)
            .promedioFacturasHora(totalFacturas > 0
                ? Math.round((double) totalFacturas / 24 * 10.0) / 10.0
                : 0.0)
            .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 10. RENDIMIENTO EMPRESAS
    // ══════════════════════════════════════════════════════════════════════
    @Override
    public RendimientoEmpresasResponse rendimientoEmpresas(Long uid, LocalDate desde, LocalDate hasta) {
        List<Tenant> tenants = obtenerTenants(uid, null);
        LocalDate[] anterior = periodoAnterior(desde, hasta);

        List<RendimientoEmpresasResponse.EmpresaRendimiento> empresas = new ArrayList<>();
        BigDecimal totalGeneral = BigDecimal.ZERO;

        for (Tenant tenant : tenants) {
            String schema = tenant.getSchemaName();
            BigDecimal totalActual   = ventasTotalesSchema(schema, desde, hasta);
            BigDecimal totalAnterior = ventasTotalesSchema(schema, anterior[0], anterior[1]);
            long facturas            = contarFacturasSchema(schema, desde, hasta);
            totalGeneral             = totalGeneral.add(totalActual);

            // Sucursales activas (con ventas en el período)
            long sucursalesActivas = 0;
            try {
                String sqlSuc = String.format("""
                    SELECT COUNT(DISTINCT sucursal_id) FROM (
                        SELECT sucursal_id FROM %s.facturas
                        WHERE fecha_emision::timestamp BETWEEN ? AND ? AND estado NOT IN ('ANULADA','ERROR')
                        UNION
                        SELECT sucursal_id FROM %s.factura_interna
                        WHERE fecha::timestamp BETWEEN ? AND ? AND estado != 'ANULADA'
                    ) s
                    """, schema, schema);
                Long sc = jdbcTemplate.queryForObject(sqlSuc, Long.class,
                    desde.atStartOfDay(), hasta.atTime(23,59,59),
                    desde.atStartOfDay(), hasta.atTime(23,59,59));
                sucursalesActivas = sc != null ? sc : 0;
            } catch (Exception e) { log.warn("Error sucursales activas: {}", e.getMessage()); }

            // Top producto
            String topProducto = "";
            try {
                String sqlTop = String.format("""
                    SELECT nombre FROM (
                        SELECT COALESCE(fd.descripcion_personalizada, fd.detalle) AS nombre, SUM(fd.cantidad) AS cant
                        FROM %s.factura_detalles fd JOIN %s.facturas f ON f.id = fd.factura_id
                        WHERE f.fecha_emision::timestamp BETWEEN ? AND ? AND f.estado NOT IN ('ANULADA','ERROR')
                        GROUP BY COALESCE(fd.descripcion_personalizada, fd.detalle)
                        UNION ALL
                        SELECT fid.nombre_producto, SUM(fid.cantidad)
                        FROM %s.factura_interna_detalle fid JOIN %s.factura_interna fi ON fi.id = fid.factura_interna_id
                        WHERE fi.fecha::timestamp BETWEEN ? AND ? AND fi.estado != 'ANULADA'
                        GROUP BY fid.nombre_producto
                    ) p ORDER BY cant DESC LIMIT 1
                    """, schema, schema, schema, schema);
                topProducto = jdbcTemplate.queryForObject(sqlTop, String.class,
                    desde.atStartOfDay(), hasta.atTime(23,59,59),
                    desde.atStartOfDay(), hasta.atTime(23,59,59));
            } catch (Exception e) { log.debug("Sin top producto para {}", schema); }

            // Top tipo de pago
            String topTipoPago = "";
            try {
                TipoPagoResponse tp = tipoPago(uid, desde, hasta, tenant.getEmpresaLegacyId(), null);
                if (!tp.getTipos().isEmpty()) topTipoPago = tp.getTipos().get(0).getTipo();
            } catch (Exception e) { log.debug("Sin top pago para {}", schema); }

            BigDecimal porcentajeCambio = BigDecimal.ZERO;
            String tendencia = "stable";
            if (totalAnterior.compareTo(BigDecimal.ZERO) > 0) {
                porcentajeCambio = totalActual.subtract(totalAnterior)
                    .divide(totalAnterior, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
                tendencia = porcentajeCambio.compareTo(BigDecimal.ZERO) > 0 ? "up"
                    : porcentajeCambio.compareTo(BigDecimal.ZERO) < 0 ? "down"
                        : "stable";
            }

            BigDecimal ticket = facturas > 0
                ? totalActual.divide(BigDecimal.valueOf(facturas), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            empresas.add(RendimientoEmpresasResponse.EmpresaRendimiento.builder()
                .empresaId(tenant.getEmpresaLegacyId())
                .empresaNombre(tenant.getNombre())
                .totalVentas(totalActual)
                .totalAnterior(totalAnterior)
                .porcentajeCambio(porcentajeCambio)
                .tendencia(tendencia)
                .ticketPromedio(ticket)
                .totalFacturas(facturas)
                .sucursalesActivas(sucursalesActivas)
                .topProducto(topProducto)
                .topTipoPago(topTipoPago)
                .participacion(BigDecimal.ZERO) // se calcula abajo
                .build());
        }

        // Calcular participación de cada empresa
        final BigDecimal tg = totalGeneral;
        empresas = empresas.stream()
            .map(e -> {
                BigDecimal participacion = tg.compareTo(BigDecimal.ZERO) > 0
                    ? e.getTotalVentas().divide(tg, 4, RoundingMode.HALF_UP)
                      .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
                e.setParticipacion(participacion);
                return e;
            })
            .sorted((a, b) -> b.getTotalVentas().compareTo(a.getTotalVentas()))
            .toList();

        return RendimientoEmpresasResponse.builder().empresas(empresas).build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 11. IMPUESTOS IVA (solo FE)
    // ══════════════════════════════════════════════════════════════════════
    @Override
    public ImpuestosResponse impuestos(Long uid, LocalDate desde, LocalDate hasta, Long empresaId) {
        List<Tenant> tenants = obtenerTenants(uid, empresaId);
        LocalDateTime desdeDT = desde.atStartOfDay();
        LocalDateTime hastaDT = hasta.atTime(23, 59, 59);

        // Mapa código tarifa → monto IVA
        Map<String, BigDecimal> ivaMap = new LinkedHashMap<>();
        List.of("08","07","04","03","02","01").forEach(c -> ivaMap.put(c, BigDecimal.ZERO));

        BigDecimal totalBaseImponible = BigDecimal.ZERO;
        BigDecimal totalExonerado     = BigDecimal.ZERO;
        long       facturasConIVA     = 0;
        long       facturasExentas    = 0;

        List<ImpuestosResponse.DesglosePorEmpresa> porEmpresa = new ArrayList<>();

        for (Tenant tenant : tenants) {
            String schema = tenant.getSchemaName();

            String sql = String.format("""
                SELECT fri.codigo_tarifa_iva,
                       SUM(fri.total_impuesto_neto)    AS monto_iva,
                       SUM(fri.total_base_imponible)   AS base,
                       SUM(fri.total_monto_exoneracion) AS exonerado
                FROM %s.factura_resumen_impuesto fri
                JOIN %s.facturas f ON f.id = fri.factura_id
                WHERE f.fecha_emision::timestamp BETWEEN ? AND ?
                  AND f.estado NOT IN ('ANULADA','ERROR')
                  AND fri.codigo_impuesto = '01'
                GROUP BY fri.codigo_tarifa_iva
                """, schema, schema);

            BigDecimal tenantIva  = BigDecimal.ZERO;
            BigDecimal tenantBase = BigDecimal.ZERO;

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, desdeDT, hastaDT);
            for (Map<String, Object> row : rows) {
                String codTarifa = (String) row.get("codigo_tarifa_iva");
                BigDecimal monto = (BigDecimal) row.get("monto_iva");
                BigDecimal base  = (BigDecimal) row.get("base");
                BigDecimal exon  = (BigDecimal) row.get("exonerado");

                if (codTarifa != null && ivaMap.containsKey(codTarifa)) {
                    ivaMap.merge(codTarifa, monto, BigDecimal::add);
                }
                tenantIva  = tenantIva.add(monto);
                tenantBase = tenantBase.add(base);
                totalExonerado = totalExonerado.add(exon != null ? exon : BigDecimal.ZERO);
            }

            totalBaseImponible = totalBaseImponible.add(tenantBase);

            porEmpresa.add(ImpuestosResponse.DesglosePorEmpresa.builder()
                .empresaId(tenant.getEmpresaLegacyId())
                .empresaNombre(tenant.getNombre())
                .totalIVA(tenantIva)
                .baseImponible(tenantBase)
                .porcentaje(BigDecimal.ZERO) // se calcula abajo
                .build());
        }

        BigDecimal totalIVA = ivaMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calcular porcentaje por empresa
        final BigDecimal tIva = totalIVA;
        porEmpresa = porEmpresa.stream().map(e -> {
            BigDecimal pct = tIva.compareTo(BigDecimal.ZERO) > 0
                ? e.getTotalIVA().divide(tIva, 4, RoundingMode.HALF_UP)
                  .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            e.setPorcentaje(pct);
            return e;
        }).toList();

        // Mapeo código → tarifa legible
        Map<String, String> tarifaLabel = Map.of(
            "01","0%","02","1%","03","2%","04","4%","07","8%","08","13%"
        );

        List<ImpuestosResponse.DesglosePorTarifa> desgloseTarifas = ivaMap.entrySet().stream()
            .map(e -> {
                BigDecimal pct = totalIVA.compareTo(BigDecimal.ZERO) > 0
                    ? e.getValue().divide(totalIVA, 4, RoundingMode.HALF_UP)
                      .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
                return ImpuestosResponse.DesglosePorTarifa.builder()
                    .tarifa(tarifaLabel.getOrDefault(e.getKey(), e.getKey()))
                    .codigoTarifa(e.getKey())
                    .montoIVA(e.getValue())
                    .baseImponible(BigDecimal.ZERO) // simplificado
                    .porcentaje(pct)
                    .build();
            })
            .sorted((a, b) -> b.getMontoIVA().compareTo(a.getMontoIVA()))
            .toList();

        return ImpuestosResponse.builder()
            .resumen(ImpuestosResponse.ResumenIVA.builder()
                .totalIVA(totalIVA)
                .totalBaseImponible(totalBaseImponible)
                .totalExonerado(totalExonerado)
                .periodoDesde(desde.toString())
                .periodoHasta(hasta.toString())
                .build())
            .desglosePorTarifa(desgloseTarifas)
            .desglosePorEmpresa(porEmpresa)
            .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 12. IMPUESTO DE SERVICIO 10% (solo FE, tipo_documento_oc = '06')
    // ══════════════════════════════════════════════════════════════════════
    @Override
    public ImpuestoServicioResponse impuestoServicio(Long uid, LocalDate desde, LocalDate hasta, Long empresaId) {
        List<Tenant> tenants = obtenerTenants(uid, empresaId);
        LocalDateTime desdeDT = desde.atStartOfDay();
        LocalDateTime hastaDT = hasta.atTime(23, 59, 59);

        BigDecimal totalServicio     = BigDecimal.ZERO;
        long totalFacturasConServicio = 0;
        long totalFacturasGeneral = 0;

        List<ImpuestoServicioResponse.DesglosePorEmpresa> porEmpresa = new ArrayList<>();
        List<ImpuestoServicioResponse.DesglosePorSucursal> porSucursal = new ArrayList<>();

        for (Tenant tenant : tenants) {
            String schema = tenant.getSchemaName();

            String sql = String.format("""
                SELECT s.id AS sucursal_id, s.nombre AS sucursal_nombre,
                       SUM(oc.monto_cargo) AS total_servicio,
                       COUNT(DISTINCT f.id) AS facturas_con_servicio
                FROM %s.facturas f
                JOIN %s.factura_otros_cargos oc ON oc.factura_id = f.id
                JOIN %s.sucursales s ON s.id = f.sucursal_id
                WHERE f.fecha_emision::timestamp BETWEEN ? AND ?
                  AND f.estado NOT IN ('ANULADA','ERROR')
                  AND oc.tipo_documento_oc = '06'
                GROUP BY s.id, s.nombre
                """, schema, schema, schema);

            BigDecimal tenantServicio = BigDecimal.ZERO;
            long tenantFacturas       = 0;

            totalFacturasGeneral += contarFacturasSchema(schema, desde, hasta);

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, desdeDT, hastaDT);
            for (Map<String, Object> row : rows) {
                BigDecimal ts = (BigDecimal) row.get("total_servicio");
                Long fc       = ((Number) row.get("facturas_con_servicio")).longValue();

                tenantServicio  = tenantServicio.add(ts != null ? ts : BigDecimal.ZERO);
                tenantFacturas += fc;

                porSucursal.add(ImpuestoServicioResponse.DesglosePorSucursal.builder()
                    .sucursalId(((Number) row.get("sucursal_id")).longValue())
                    .sucursalNombre((String) row.get("sucursal_nombre"))
                    .empresaNombre(tenant.getNombre())
                    .totalServicio(ts != null ? ts : BigDecimal.ZERO)
                    .facturasConServicio(fc)
                    .porcentaje(BigDecimal.ZERO) // se calcula abajo
                    .build());
            }

            totalServicio          = totalServicio.add(tenantServicio);
            totalFacturasConServicio += tenantFacturas;

            porEmpresa.add(ImpuestoServicioResponse.DesglosePorEmpresa.builder()
                .empresaId(tenant.getEmpresaLegacyId())
                .empresaNombre(tenant.getNombre())
                .totalServicio(tenantServicio)
                .facturasConServicio(tenantFacturas)
                .porcentaje(BigDecimal.ZERO) // se calcula abajo
                .build());
        }

        // Calcular porcentajes
        final BigDecimal ts = totalServicio;
        porEmpresa = porEmpresa.stream().map(e -> {
            e.setPorcentaje(ts.compareTo(BigDecimal.ZERO) > 0
                ? e.getTotalServicio().divide(ts, 4, RoundingMode.HALF_UP)
                  .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
            return e;
        }).toList();

        porSucursal = porSucursal.stream()
            .map(s -> {
                s.setPorcentaje(ts.compareTo(BigDecimal.ZERO) > 0
                    ? s.getTotalServicio().divide(ts, 4, RoundingMode.HALF_UP)
                      .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
                return s;
            })
            .sorted((a, b) -> b.getTotalServicio().compareTo(a.getTotalServicio()))
            .toList();

        BigDecimal promedioFactura = totalFacturasConServicio > 0
            ? totalServicio.divide(BigDecimal.valueOf(totalFacturasConServicio), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        BigDecimal porcentajeCobertura = totalFacturasGeneral > 0
            ? BigDecimal.valueOf(totalFacturasConServicio)
              .divide(BigDecimal.valueOf(totalFacturasGeneral), 4, RoundingMode.HALF_UP)
              .multiply(new BigDecimal("100"))
              .setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return ImpuestoServicioResponse.builder()
            .resumen(ImpuestoServicioResponse.ResumenServicio.builder()
                .totalServicio(totalServicio)
                .totalFacturasConServicio(totalFacturasConServicio)
                .promedioServicioFactura(promedioFactura)
                .totalFacturas(totalFacturasGeneral)
                .porcentajeCobertura(porcentajeCobertura)
                .periodoDesde(desde.toString())
                .periodoHasta(hasta.toString())
                .build())
            .desglosePorEmpresa(porEmpresa)
            .desglosePorSucursal(porSucursal)
            .build();
    }
 
    // ══════════════════════════════════════════════════════════════════════
    // MÉTODOS AUXILIARES JDBC (schema explícito)
    // ══════════════════════════════════════════════════════════════════════
 
    /**
     * Total de ventas en un schema para un rango de fechas.
     * Suma facturas electrónicas + facturas internas.
     */
    private BigDecimal ventasTotalesSchema(String schema, LocalDate desde, LocalDate hasta) {
        String sql = String.format("""
            SELECT COALESCE(SUM(total), 0) FROM (
                SELECT total_comprobante AS total
                FROM %s.facturas
                WHERE fecha_emision::timestamp BETWEEN ? AND ?
                  AND estado NOT IN ('ANULADA', 'ERROR')
                UNION ALL
                SELECT total AS total
                FROM %s.factura_interna
                WHERE fecha::timestamp BETWEEN ? AND ?
            ) v
            """, schema, schema);

        LocalDateTime desdeDT = desde.atStartOfDay();
        LocalDateTime hastaDT = hasta.atTime(23, 59, 59);

        BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, desdeDT, hastaDT, desdeDT, hastaDT);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * Cuenta total de facturas (FE + internas) en un schema.
     */
    private long contarFacturasSchema(String schema, LocalDate desde, LocalDate hasta) {
        String sql = String.format("""
            SELECT COUNT(*) FROM (
                SELECT id FROM %s.facturas
                WHERE fecha_emision::timestamp BETWEEN ? AND ?
                  AND estado NOT IN ('ANULADA', 'ERROR')
                UNION ALL
                SELECT id FROM %s.factura_interna
                WHERE fecha::timestamp BETWEEN ? AND ?
            ) f
            """, schema, schema);

        LocalDateTime desdeDT = desde.atStartOfDay();
        LocalDateTime hastaDT = hasta.atTime(23, 59, 59);

        Long count = jdbcTemplate.queryForObject(sql, Long.class, desdeDT, hastaDT, desdeDT, hastaDT);
        return count != null ? count : 0L;
    }

    /**
     * Cuenta registros de una tabla en un schema.
     */
    private long contarRegistros(String schema, String tabla) {
        try {
            String sql = String.format("SELECT COUNT(*) FROM %s.%s WHERE activo = true", schema, tabla);
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("Error contando {} en schema {}: {}", tabla, schema, e.getMessage());
            return 0L;
        }
    }

    /**
     * Genera lista de fechas para el rango, según agrupación.
     * Garantiza que todos los arrays de puntos tengan las mismas fechas.
     */
    private List<String> generarFechas(LocalDate desde, LocalDate hasta, String agrupacion) {
        List<String> fechas = new ArrayList<>();
        LocalDate cursor = desde;
        while (!cursor.isAfter(hasta)) {
            fechas.add(switch (agrupacion) {
                case "semana" -> cursor.with(java.time.DayOfWeek.MONDAY).toString();
                case "mes"    -> cursor.withDayOfMonth(1).toString();
                default       -> cursor.toString();
            });
            cursor = switch (agrupacion) {
                case "semana" -> cursor.plusWeeks(1);
                case "mes"    -> cursor.plusMonths(1);
                default       -> cursor.plusDays(1);
            };
        }
        return fechas.stream().distinct().toList();
    }
}