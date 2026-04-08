package com.snnsoluciones.backnathbitpos.service.reportes.impl;

import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteIvaComprasLineaDTO;
import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteIvaComprasRequest;
import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteIvaComprasResponse;
import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteIvaComprasRowMapper;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.service.reportes.ReporteIvaComprasService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteIvaComprasServiceImpl implements ReporteIvaComprasService {

    private final SucursalRepository         sucursalRepository;
    private final JdbcTemplate               jdbcTemplate;
    private final ReporteIvaComprasRowMapper rowMapper;

    private static final DateTimeFormatter FMT_DT     = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_DATE   = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ─────────────────────────────────────────────────────────────────────────
    //  generarReporte
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ReporteIvaComprasResponse generarReporte(ReporteIvaComprasRequest request) {
        request.aplicarDefaults();

        log.info("Reporte IVA Compras — sucursal={} | emision=[{} -> {}] | estado={} | tipos={}",
            request.getSucursalId(),
            request.getFechaEmisionDesde(), request.getFechaEmisionHasta(),
            request.getEstadoInterno(), request.getTiposDocumento());

        validarSucursal(request.getSucursalId());
        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
            .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada"));

        QueryConParams qcp = buildQuery(request);

        List<ReporteIvaComprasLineaDTO> lineas = jdbcTemplate.query(
            qcp.sql(), rowMapper, qcp.params().toArray()
        );

        log.info("Reporte IVA Compras — {} documentos para sucursal {}", lineas.size(), request.getSucursalId());

        ReporteIvaComprasResponse response = ReporteIvaComprasResponse.builder()
            .empresaNombre(sucursal.getEmpresa().getNombreRazonSocial())
            .empresaIdentificacion(sucursal.getEmpresa().getIdentificacion())
            .sucursalNombre(sucursal.getNombre())
            .fechaEmisionDesde(request.getFechaEmisionDesde())
            .fechaEmisionHasta(request.getFechaEmisionHasta())
            .estadoInterno(request.getEstadoInterno())
            .tiposDocumentoConsultados(request.getTiposDocumento())
            .fechaGeneracion(LocalDateTime.now())
            .lineas(lineas)
            .build();

        response.calcularTotales();
        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  buildQuery  — SQL con agregación condicional de IVA por tarifa
    // ─────────────────────────────────────────────────────────────────────────

    private QueryConParams buildQuery(ReporteIvaComprasRequest req) {

        StringBuilder sql = new StringBuilder("""
            SELECT
                fr.tipo_documento,
                fr.clave,
                fr.numero_consecutivo            AS consecutivo,
                fr.proveedor_nombre,
                fr.proveedor_identificacion,
                fr.fecha_emision,
                COALESCE(fr.total_gravado,      0) AS total_gravado,
                COALESCE(fr.total_exento,       0) AS total_exento,
                COALESCE(fr.total_exonerado,    0) AS total_exonerado,
                COALESCE(fr.total_venta_neta,   0) AS total_venta_neta,
                COALESCE(fr.total_impuesto,     0) AS total_impuesto,
                COALESCE(fr.total_descuentos,   0) AS total_descuentos,
                COALESCE(fr.total_otros_cargos, 0) AS total_otros_cargos,
                COALESCE(fr.total_comprobante,  0) AS total_comprobante,
                -- IVA 0%
               COALESCE(SUM(CASE
                   WHEN di.codigo_impuesto = '01'
                    AND di.codigo_tarifa IN ('01','05','10','11')
                   THEN COALESCE(NULLIF(di.impuesto_neto, 0), di.monto, 0)
                   ELSE 0 END), 0) AS iva_0,
               -- IVA 1%
               COALESCE(SUM(CASE
                   WHEN di.codigo_impuesto = '01' AND di.codigo_tarifa = '02'
                   THEN COALESCE(NULLIF(di.impuesto_neto, 0), di.monto, 0)
                   ELSE 0 END), 0) AS iva_1,
               -- IVA 2%
               COALESCE(SUM(CASE
                   WHEN di.codigo_impuesto = '01' AND di.codigo_tarifa = '03'
                   THEN COALESCE(NULLIF(di.impuesto_neto, 0), di.monto, 0)
                   ELSE 0 END), 0) AS iva_2,
               -- IVA 4%
               COALESCE(SUM(CASE
                   WHEN di.codigo_impuesto = '01' AND di.codigo_tarifa IN ('04','06')
                   THEN COALESCE(NULLIF(di.impuesto_neto, 0), di.monto, 0)
                   ELSE 0 END), 0) AS iva_4,
               -- IVA 8%
               COALESCE(SUM(CASE
                   WHEN di.codigo_impuesto = '01' AND di.codigo_tarifa = '07'
                   THEN COALESCE(NULLIF(di.impuesto_neto, 0), di.monto, 0)
                   ELSE 0 END), 0) AS iva_8,
               -- IVA 13%
               COALESCE(SUM(CASE
                   WHEN di.codigo_impuesto = '01' AND di.codigo_tarifa = '08'
                   THEN COALESCE(NULLIF(di.impuesto_neto, 0), di.monto, 0)
                   ELSE 0 END), 0) AS iva_13,
               -- Otros impuestos: residual para que siempre sume a fr.total_impuesto
             GREATEST(0,
                 COALESCE(fr.total_impuesto, 0)
                 - COALESCE(SUM(CASE
                     WHEN di.codigo_impuesto = '01' AND di.codigo_tarifa IN ('01','05','10','11')
                     THEN COALESCE(NULLIF(di.impuesto_neto, 0), di.monto, 0) ELSE 0 END), 0)
                 - COALESCE(SUM(CASE
                     WHEN di.codigo_impuesto = '01' AND di.codigo_tarifa = '02'
                     THEN COALESCE(NULLIF(di.impuesto_neto, 0), di.monto, 0) ELSE 0 END), 0)
                 - COALESCE(SUM(CASE
                     WHEN di.codigo_impuesto = '01' AND di.codigo_tarifa = '03'
                     THEN COALESCE(NULLIF(di.impuesto_neto, 0), di.monto, 0) ELSE 0 END), 0)
                 - COALESCE(SUM(CASE
                     WHEN di.codigo_impuesto = '01' AND di.codigo_tarifa IN ('04','06')
                     THEN COALESCE(NULLIF(di.impuesto_neto, 0), di.monto, 0) ELSE 0 END), 0)
                 - COALESCE(SUM(CASE
                     WHEN di.codigo_impuesto = '01' AND di.codigo_tarifa = '07'
                     THEN COALESCE(NULLIF(di.impuesto_neto, 0), di.monto, 0) ELSE 0 END), 0)
                 - COALESCE(SUM(CASE
                     WHEN di.codigo_impuesto = '01' AND di.codigo_tarifa = '08'
                     THEN COALESCE(NULLIF(di.impuesto_neto, 0), di.monto, 0) ELSE 0 END), 0)
             ) AS otros_impuestos
            FROM  facturas_recepcion fr
            LEFT JOIN facturas_recepcion_detalles         frd ON frd.factura_recepcion_id        = fr.id
            LEFT JOIN facturas_recepcion_detalles_impuestos di ON di.factura_recepcion_detalle_id = frd.id
            WHERE fr.sucursal_id   = ?
              AND fr.estado_interno = ?
              AND fr.fecha_emision >= ?
              AND fr.fecha_emision <= ?
            """);

        List<Object> params = new ArrayList<>();
        params.add(req.getSucursalId());
        params.add(req.getEstadoInterno());
        params.add(req.getFechaDesdeDateTime());
        params.add(req.getFechaHastaDateTime());

        // Filtro dinámico de tipos de documento
        if (req.getTiposDocumento() != null && !req.getTiposDocumento().isEmpty()) {
            String placeholders = String.join(",",
                req.getTiposDocumento().stream().map(t -> "?").toList());
            sql.append(" AND fr.tipo_documento IN (").append(placeholders).append(")");
            params.addAll(req.getTiposDocumento());
        }

        sql.append("""
            GROUP BY
                fr.id,
                fr.tipo_documento,
                fr.clave,
                fr.numero_consecutivo,
                fr.proveedor_nombre,
                fr.proveedor_identificacion,
                fr.fecha_emision,
                fr.total_gravado,
                fr.total_exento,
                fr.total_exonerado,
                fr.total_venta_neta,
                fr.total_impuesto,
                fr.total_descuentos,
                fr.total_otros_cargos,
                fr.total_comprobante
            ORDER BY fr.fecha_emision ASC
            """);

        return new QueryConParams(sql.toString(), params);
    }

    private record QueryConParams(String sql, List<Object> params) {}

    // ─────────────────────────────────────────────────────────────────────────
    //  validarSucursal
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void validarSucursal(Long sucursalId) {
        if (!sucursalRepository.existsById(sucursalId)) {
            throw new EntityNotFoundException("Sucursal no encontrada con ID: " + sucursalId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  generarExcel
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] generarExcel(ReporteIvaComprasRequest request) {
        ReporteIvaComprasResponse datos = generarReporte(request);

        log.info("Generando Excel IVA Compras — {} documentos, sucursal {}",
            datos.getTotalDocumentos(), request.getSucursalId());

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Estilos
            CellStyle titleStyle    = estTitulo(wb);
            CellStyle subStyle      = estSubtitulo(wb);
            CellStyle headerStyle   = estHeader(wb);
            CellStyle dataStyle     = estDato(wb);
            CellStyle moneyStyle    = estMoneda(wb);
            CellStyle dateStyle     = estFecha(wb);
            CellStyle totalStyle    = estTotal(wb);
            CellStyle ncDataStyle   = estNcDato(wb);
            CellStyle ncMoneyStyle  = estNcMoneda(wb);

            Sheet sheet = wb.createSheet("IVA Compras");
            sheet.setDefaultColumnWidth(16);

            int row = 0;
            row = crearTitulo(sheet, row, titleStyle, subStyle, datos);
            row = crearResumen(sheet, row, datos, dataStyle, moneyStyle);
            row++; // espacio
            row = crearHeaders(sheet, row, headerStyle);
            row = crearFilas(sheet, row, datos.getLineas(), dataStyle, moneyStyle, dateStyle, ncDataStyle, ncMoneyStyle);
            crearTotales(sheet, row, datos, totalStyle);

            for (int i = 0; i <= 17; i++) sheet.autoSizeColumn(i);

            wb.write(out);
            log.info("Excel IVA Compras generado: {} bytes", out.size());
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Error generando Excel IVA Compras", e);
            throw new RuntimeException("Error al generar Excel: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Secciones del Excel
    // ─────────────────────────────────────────────────────────────────────────

    private int crearTitulo(Sheet sheet, int row, CellStyle title, CellStyle sub,
                             ReporteIvaComprasResponse d) {
        addRow(sheet, row++, title, 0, 17,
            d.getEmpresaNombre() + " — " + d.getEmpresaIdentificacion());
        addRow(sheet, row++, sub, 0, 17,
            "Reporte de IVA por Tarifa — COMPRAS — " + d.getSucursalNombre());
        addRow(sheet, row++, null, 0, 17, String.format(
            "Período: %s al %s   |   Estado: %s   |   Generado: %s",
            d.getFechaEmisionDesde().format(FMT_DATE),
            d.getFechaEmisionHasta().format(FMT_DATE),
            d.getEstadoInterno(),
            LocalDateTime.now().format(FMT_DT)));
        row++; // espacio
        return row;
    }

    private int crearResumen(Sheet sheet, int row, ReporteIvaComprasResponse d,
                              CellStyle ds, CellStyle ms) {
        // Contadores
        row = addKv(sheet, row, ds, "Total documentos",   String.valueOf(d.getTotalDocumentos()));
        row = addKv(sheet, row, ds, "Facturas",           String.valueOf(d.getCantidadFacturas()));
        row = addKv(sheet, row, ds, "Tiquetes",           String.valueOf(d.getCantidadTiquetes()));
        row = addKv(sheet, row, ds, "Notas de crédito",  String.valueOf(d.getCantidadNotasCredito()));

        // Totales IVA
        Object[][] ivas = {
            {"IVA 0%",          d.getTotalIva0()},
            {"IVA 1%",          d.getTotalIva1()},
            {"IVA 2%",          d.getTotalIva2()},
            {"IVA 4%",          d.getTotalIva4()},
            {"IVA 8%",          d.getTotalIva8()},
            {"IVA 13%",         d.getTotalIva13()},
            {"Otros Impuestos", d.getTotalOtrosImpuestos()},
            {"Total Gravado",   d.getTotalGravado()},
            {"Total Exento",    d.getTotalExento()},
            {"Total Exonerado", d.getTotalExonerado()},
            {"Venta Neta",      d.getTotalVentaNeta()},
            {"Total Impuestos", d.getTotalImpuestos()},
            {"Descuentos",      d.getTotalDescuentos()},
            {"Otros Cargos",    d.getTotalOtrosCargos()},
            {"TOTAL GENERAL",   d.getTotalGeneral()},
        };
        for (Object[] kv : ivas) {
            Row r = sheet.createRow(row++);
            Cell lbl = r.createCell(0); lbl.setCellValue((String) kv[0]); lbl.setCellStyle(ds);
            Cell val = r.createCell(1);
            BigDecimal bd = kv[1] != null ? (BigDecimal) kv[1] : BigDecimal.ZERO;
            val.setCellValue(bd.doubleValue());
            val.setCellStyle(ms);
        }
        return row;
    }

    private int crearHeaders(Sheet sheet, int row, CellStyle hs) {
        Row r = sheet.createRow(row++);
        r.setHeightInPoints(22);
        String[] cols = {
            "Tipo", "Consecutivo", "Proveedor", "Identificación", "Fecha",
            "IVA 0%", "IVA 1%", "IVA 2%", "IVA 4%", "IVA 8%", "IVA 13%", "Otros Imp.",
            "Gravado", "Exento", "Exonerado",
            "Venta Neta", "Total IVA", "Descuentos", "Otros Cargos", "TOTAL"
        };
        for (int i = 0; i < cols.length; i++) {
            Cell c = r.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(hs);
        }
        return row;
    }

    private int crearFilas(Sheet sheet, int row, List<ReporteIvaComprasLineaDTO> lineas,
                            CellStyle ds, CellStyle ms, CellStyle dateStyle,
                            CellStyle ncDs, CellStyle ncMs) {
        if (lineas == null) return row;
        for (ReporteIvaComprasLineaDTO l : lineas) {
            Row r = sheet.createRow(row++);
            boolean nc = "NOTA_CREDITO".equals(l.getTipoDocumento());
            CellStyle d = nc ? ncDs : ds;
            CellStyle m = nc ? ncMs : ms;

            setStr(r, 0,  tipoLabel(l.getTipoDocumento()), d);
            setStr(r, 1,  l.getConsecutivo(),              d);
            setStr(r, 2,  l.getProveedorNombre(),          d);
            setStr(r, 3,  l.getProveedorIdentificacion(),  d);
            Cell fc = r.createCell(4);
            fc.setCellValue(l.getFechaEmision() != null ? l.getFechaEmision().format(FMT_DT) : "");
            fc.setCellStyle(dateStyle);

            setMoney(r,  5, l.getIva0(),            m);
            setMoney(r,  6, l.getIva1(),            m);
            setMoney(r,  7, l.getIva2(),            m);
            setMoney(r,  8, l.getIva4(),            m);
            setMoney(r,  9, l.getIva8(),            m);
            setMoney(r, 10, l.getIva13(),           m);
            setMoney(r, 11, l.getOtrosImpuestos(),  m);
            setMoney(r, 12, l.getTotalGravado(),    m);
            setMoney(r, 13, l.getTotalExento(),     m);
            setMoney(r, 14, l.getTotalExonerado(),  m);
            setMoney(r, 15, l.getTotalVentaNeta(),  m);
            setMoney(r, 16, l.getTotalImpuesto(),   m);
            setMoney(r, 17, l.getTotalDescuentos(), m);
            setMoney(r, 18, l.getTotalOtrosCargos(),m);
            setMoney(r, 19, l.getTotalComprobante(),m);
        }
        return row;
    }

    private void crearTotales(Sheet sheet, int row, ReporteIvaComprasResponse d, CellStyle ts) {
        Row r = sheet.createRow(row);
        Cell lbl = r.createCell(0); lbl.setCellValue("TOTALES"); lbl.setCellStyle(ts);
        sheet.addMergedRegion(new CellRangeAddress(row, row, 0, 4));

        setMoneyTotal(r,  5, d.getTotalIva0(),           ts);
        setMoneyTotal(r,  6, d.getTotalIva1(),           ts);
        setMoneyTotal(r,  7, d.getTotalIva2(),           ts);
        setMoneyTotal(r,  8, d.getTotalIva4(),           ts);
        setMoneyTotal(r,  9, d.getTotalIva8(),           ts);
        setMoneyTotal(r, 10, d.getTotalIva13(),          ts);
        setMoneyTotal(r, 11, d.getTotalOtrosImpuestos(), ts);
        setMoneyTotal(r, 12, d.getTotalGravado(),        ts);
        setMoneyTotal(r, 13, d.getTotalExento(),         ts);
        setMoneyTotal(r, 14, d.getTotalExonerado(),      ts);
        setMoneyTotal(r, 15, d.getTotalVentaNeta(),      ts);
        setMoneyTotal(r, 16, d.getTotalImpuestos(),      ts);
        setMoneyTotal(r, 17, d.getTotalDescuentos(),     ts);
        setMoneyTotal(r, 18, d.getTotalOtrosCargos(),    ts);
        setMoneyTotal(r, 19, d.getTotalGeneral(),        ts);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Utilidades Excel
    // ─────────────────────────────────────────────────────────────────────────

    private void addRow(Sheet s, int row, CellStyle style, int from, int to, String val) {
        Row r = s.createRow(row);
        Cell c = r.createCell(from);
        c.setCellValue(val);
        if (style != null) c.setCellStyle(style);
        if (to > from) s.addMergedRegion(new CellRangeAddress(row, row, from, to));
    }

    private int addKv(Sheet s, int row, CellStyle cs, String label, String value) {
        Row r = s.createRow(row);
        Cell l = r.createCell(0); l.setCellValue(label); l.setCellStyle(cs);
        Cell v = r.createCell(1); v.setCellValue(value);  v.setCellStyle(cs);
        return row + 1;
    }

    private void setStr(Row r, int col, String val, CellStyle s) {
        Cell c = r.createCell(col);
        c.setCellValue(val != null ? val : "");
        c.setCellStyle(s);
    }

    private void setMoney(Row r, int col, BigDecimal val, CellStyle s) {
        Cell c = r.createCell(col);
        c.setCellValue(val != null ? val.doubleValue() : 0.0);
        c.setCellStyle(s);
    }

    private void setMoneyTotal(Row r, int col, BigDecimal val, CellStyle s) {
        Cell c = r.createCell(col);
        c.setCellValue(val != null ? val.doubleValue() : 0.0);
        c.setCellStyle(s);
    }

    private String tipoLabel(String tipo) {
        if (tipo == null) return "";
        return switch (tipo) {
            case "FACTURA_ELECTRONICA" -> "Factura FE";
            case "TIQUETE_ELECTRONICO" -> "Tiquete TE";
            case "NOTA_CREDITO"        -> "Nota Crédito";
            case "NOTA_DEBITO"         -> "Nota Débito";
            default -> tipo;
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Fábrica de estilos POI
    // ─────────────────────────────────────────────────────────────────────────

    private static final byte[] AZUL   = {(byte)31,  (byte)73, (byte)125};
    private static final byte[] ROJO   = {(byte)192, (byte)0,  (byte)0};

    private CellStyle estTitulo(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)14);
        s.setFont(f); return s;
    }

    private CellStyle estSubtitulo(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)12);
        s.setFont(f); return s;
    }

    private CellStyle estHeader(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(AZUL, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        borde(s); return s;
    }

    private CellStyle estDato(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle(); borde(s); return s;
    }

    private CellStyle estMoneda(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setDataFormat(wb.createDataFormat().getFormat("₡#,##0.00"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        borde(s); return s;
    }

    private CellStyle estFecha(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle(); borde(s); return s;
    }

    private CellStyle estTotal(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(AZUL, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setDataFormat(wb.createDataFormat().getFormat("₡#,##0.00"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        borde(s); return s;
    }

    private CellStyle estNcDato(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setItalic(true);
        f.setColor(new XSSFColor(ROJO, null));
        s.setFont(f); borde(s); return s;
    }

    private CellStyle estNcMoneda(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setItalic(true);
        f.setColor(new XSSFColor(ROJO, null));
        s.setFont(f);
        s.setDataFormat(wb.createDataFormat().getFormat("₡#,##0.00"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        borde(s); return s;
    }

    private void borde(CellStyle s) {
        s.setBorderBottom(BorderStyle.THIN); s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);   s.setBorderRight(BorderStyle.THIN);
    }
}