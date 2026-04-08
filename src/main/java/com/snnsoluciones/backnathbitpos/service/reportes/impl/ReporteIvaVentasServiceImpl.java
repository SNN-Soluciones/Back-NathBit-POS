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
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import java.io.ByteArrayOutputStream;
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
    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_PERIODO = DateTimeFormatter.ofPattern("dd/MM/yyyy");


    @Override
    @Transactional(readOnly = true)
    public byte[] generarExcel(ReporteIvaVentasRequest request) {
        // Reutilizamos exactamente la misma lógica del reporte JSON
        ReporteIvaVentasResponse datos = generarReporte(request);

        log.info("Generando Excel IVA — {} documentos, sucursal {}",
            datos.getTotalDocumentos(), request.getSucursalId());

        try (XSSFWorkbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ── Estilos ──────────────────────────────────────────────────
            CellStyle titleStyle    = crearEstiloTitulo(workbook);
            CellStyle subtitleStyle = crearEstiloSubtitulo(workbook);
            CellStyle headerStyle   = crearEstiloHeader(workbook);
            CellStyle dataStyle     = crearEstiloDato(workbook);
            CellStyle moneyStyle    = crearEstiloMoneda(workbook);
            CellStyle dateStyle     = crearEstiloFecha(workbook);
            CellStyle totalStyle    = crearEstiloTotal(workbook);
            CellStyle ncStyle       = crearEstiloNotaCredito(workbook);
            CellStyle ncMoneyStyle  = crearEstiloNotaCreditoMoneda(workbook);

            Sheet sheet = workbook.createSheet("IVA Ventas");
            sheet.setDefaultColumnWidth(16);

            int rowNum = 0;

            // ── Título ───────────────────────────────────────────────────
            rowNum = crearTitulo(sheet, rowNum, titleStyle, subtitleStyle, datos);

            // ── Resumen de totales ────────────────────────────────────────
            rowNum = crearResumen(sheet, rowNum, datos, moneyStyle, dataStyle);

            rowNum++; // espacio

            // ── Headers de la tabla ───────────────────────────────────────
            rowNum = crearHeaders(sheet, rowNum, headerStyle);

            // ── Filas de datos ────────────────────────────────────────────
            rowNum = crearFilas(sheet, rowNum, datos.getLineas(), dataStyle, moneyStyle, dateStyle, ncStyle, ncMoneyStyle);

            // ── Fila de totales ───────────────────────────────────────────
            crearFilaTotales(sheet, rowNum, datos, totalStyle, moneyStyle);

            // ── Autosize columnas ─────────────────────────────────────────
            for (int i = 0; i <= 12; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            log.info("Excel IVA generado: {} bytes", out.size());
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Error generando Excel IVA", e);
            throw new RuntimeException("Error al generar Excel: " + e.getMessage(), e);
        }
    }

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
                    THEN i.impuesto_neto ELSE 0 END), 0)
                    * CASE WHEN f.tipo_documento = 'NOTA_CREDITO' THEN -1 ELSE 1 END AS iva_0,
    
                COALESCE(SUM(CASE WHEN i.tarifa IN (0.01, 1.00)
                    THEN i.impuesto_neto ELSE 0 END), 0)
                    * CASE WHEN f.tipo_documento = 'NOTA_CREDITO' THEN -1 ELSE 1 END AS iva_1,
    
                COALESCE(SUM(CASE WHEN i.tarifa IN (0.02, 2.00)
                    THEN i.impuesto_neto ELSE 0 END), 0)
                    * CASE WHEN f.tipo_documento = 'NOTA_CREDITO' THEN -1 ELSE 1 END AS iva_2,
    
                COALESCE(SUM(CASE WHEN i.tarifa IN (0.04, 4.00)
                    THEN i.impuesto_neto ELSE 0 END), 0)
                    * CASE WHEN f.tipo_documento = 'NOTA_CREDITO' THEN -1 ELSE 1 END AS iva_4,
    
                COALESCE(SUM(CASE WHEN i.tarifa IN (0.08, 8.00)
                    THEN i.impuesto_neto ELSE 0 END), 0)
                    * CASE WHEN f.tipo_documento = 'NOTA_CREDITO' THEN -1 ELSE 1 END AS iva_8,
    
                COALESCE(SUM(CASE WHEN i.tarifa IN (0.13, 13.00)
                    THEN i.impuesto_neto ELSE 0 END), 0)
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

    private int crearTitulo(Sheet sheet, int rowNum,
        CellStyle titleStyle, CellStyle subtitleStyle,
        ReporteIvaVentasResponse datos) {

        // Empresa
        Row r0 = sheet.createRow(rowNum++);
        Cell c0 = r0.createCell(0);
        c0.setCellValue(datos.getEmpresaNombre() + " — " + datos.getEmpresaIdentificacion());
        c0.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(r0.getRowNum(), r0.getRowNum(), 0, 12));

        // Sucursal + reporte
        Row r1 = sheet.createRow(rowNum++);
        Cell c1 = r1.createCell(0);
        c1.setCellValue("Reporte de IVA por Tarifa — " + datos.getSucursalNombre());
        c1.setCellStyle(subtitleStyle);
        sheet.addMergedRegion(new CellRangeAddress(r1.getRowNum(), r1.getRowNum(), 0, 12));

        // Período
        Row r2 = sheet.createRow(rowNum++);
        Cell c2 = r2.createCell(0);
        String periodo = String.format("Período: %s al %s   |   Estado: %s   |   Generado: %s",
            datos.getFechaEmisionDesde().format(FMT_PERIODO),
            datos.getFechaEmisionHasta().format(FMT_PERIODO),
            datos.getEstadoBitacora(),
            LocalDateTime.now().format(FMT_FECHA));
        c2.setCellValue(periodo);
        sheet.addMergedRegion(new CellRangeAddress(r2.getRowNum(), r2.getRowNum(), 0, 12));

        rowNum++; // espacio
        return rowNum;
    }

    private int crearResumen(Sheet sheet, int rowNum,
        ReporteIvaVentasResponse datos,
        CellStyle moneyStyle, CellStyle dataStyle) {

        String[][] resumen = {
            { "Total documentos",  String.valueOf(datos.getTotalDocumentos()) },
            { "Facturas",          String.valueOf(datos.getCantidadFacturas()) },
            { "Tiquetes",          String.valueOf(datos.getCantidadTiquetes()) },
            { "Notas de crédito",  String.valueOf(datos.getCantidadNotasCredito()) },
        };

        for (String[] par : resumen) {
            Row r = sheet.createRow(rowNum++);
            Cell label = r.createCell(0); label.setCellValue(par[0]); label.setCellStyle(dataStyle);
            Cell val   = r.createCell(1); val.setCellValue(par[1]);   val.setCellStyle(dataStyle);
        }

        // Totales monetarios del resumen
        Object[][] totales = {
            { "IVA 0%",       datos.getTotalIva0()      },
            { "IVA 1%",       datos.getTotalIva1()      },
            { "IVA 2%",       datos.getTotalIva2()      },
            { "IVA 4%",       datos.getTotalIva4()      },
            { "IVA 8%",       datos.getTotalIva8()      },
            { "IVA 13%",      datos.getTotalIva13()     },
            { "Total Neto",   datos.getTotalNeto()      },
            { "Total IVA",    datos.getTotalImpuestos() },
            { "Descuentos",   datos.getTotalDescuentos()},
            { "TOTAL GENERAL",datos.getTotalGeneral()   },
        };

        for (Object[] par : totales) {
            Row r = sheet.createRow(rowNum++);
            Cell label = r.createCell(0); label.setCellValue((String) par[0]); label.setCellStyle(dataStyle);
            Cell val   = r.createCell(1);
            BigDecimal bd = par[1] != null ? (BigDecimal) par[1] : BigDecimal.ZERO;
            val.setCellValue(bd.doubleValue());
            val.setCellStyle(moneyStyle);
        }

        return rowNum;
    }

    private int crearHeaders(Sheet sheet, int rowNum, CellStyle headerStyle) {
        Row r = sheet.createRow(rowNum++);
        r.setHeightInPoints(20);

        String[] cols = {
            "Tipo",           // 0
            "Consecutivo",    // 1
            "Clave",          // 2
            "Cliente",        // 3
            "Identificación", // 4
            "Fecha Emisión",  // 5
            "IVA 0%",         // 6
            "IVA 1%",         // 7
            "IVA 2%",         // 8
            "IVA 4%",         // 9
            "IVA 8%",         // 10
            "IVA 13%",        // 11
            "Total Neto",     // 12
            "Total IVA",      // 13
            "Descuentos",     // 14
            "Total"           // 15
        };

        for (int i = 0; i < cols.length; i++) {
            Cell c = r.createCell(i);
            c.setCellValue(cols[i]);
            c.setCellStyle(headerStyle);
        }
        return rowNum;
    }

    private int crearFilas(Sheet sheet, int rowNum,
        List<ReporteIvaVentasLineaDTO> lineas,
        CellStyle dataStyle, CellStyle moneyStyle, CellStyle dateStyle,
        CellStyle ncStyle, CellStyle ncMoneyStyle) {

        if (lineas == null) return rowNum;

        for (ReporteIvaVentasLineaDTO l : lineas) {
            Row r = sheet.createRow(rowNum++);
            boolean esNC = "NOTA_CREDITO".equals(l.getTipoDocumento());

            CellStyle ds = esNC ? ncStyle      : dataStyle;
            CellStyle ms = esNC ? ncMoneyStyle : moneyStyle;

            setCell(r, 0,  tipoDocumentoLabel(l.getTipoDocumento()), ds);
            setCell(r, 1,  l.getConsecutivo(),                       ds);
            setCell(r, 2,  l.getClave(),                             ds);
            setCell(r, 3,  l.getClienteNombre(),                     ds);
            setCell(r, 4,  l.getClienteIdentificacion(),             ds);

            Cell fechaCell = r.createCell(5);
            fechaCell.setCellValue(l.getFechaEmision() != null
                ? l.getFechaEmision().format(FMT_FECHA) : "");
            fechaCell.setCellStyle(dateStyle);

            setMoney(r,  6, l.getIva0(),          ms);
            setMoney(r,  7, l.getIva1(),          ms);
            setMoney(r,  8, l.getIva2(),          ms);
            setMoney(r,  9, l.getIva4(),          ms);
            setMoney(r, 10, l.getIva8(),          ms);
            setMoney(r, 11, l.getIva13(),         ms);
            setMoney(r, 12, l.getTotalNeto(),     ms);
            setMoney(r, 13, l.getTotalImpuestos(),ms);
            setMoney(r, 14, l.getDescuentos(),    ms);
            setMoney(r, 15, l.getTotal(),         ms);
        }
        return rowNum;
    }

    private void crearFilaTotales(Sheet sheet, int rowNum,
        ReporteIvaVentasResponse datos,
        CellStyle totalStyle, CellStyle moneyStyle) {
        Row r = sheet.createRow(rowNum);

        Cell label = r.createCell(0);
        label.setCellValue("TOTALES");
        label.setCellStyle(totalStyle);
        // Merge columnas 0-5
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 5));

        setMoneyTotal(r,  6, datos.getTotalIva0(),       totalStyle);
        setMoneyTotal(r,  7, datos.getTotalIva1(),       totalStyle);
        setMoneyTotal(r,  8, datos.getTotalIva2(),       totalStyle);
        setMoneyTotal(r,  9, datos.getTotalIva4(),       totalStyle);
        setMoneyTotal(r, 10, datos.getTotalIva8(),       totalStyle);
        setMoneyTotal(r, 11, datos.getTotalIva13(),      totalStyle);
        setMoneyTotal(r, 12, datos.getTotalNeto(),       totalStyle);
        setMoneyTotal(r, 13, datos.getTotalImpuestos(),  totalStyle);
        setMoneyTotal(r, 14, datos.getTotalDescuentos(), totalStyle);
        setMoneyTotal(r, 15, datos.getTotalGeneral(),    totalStyle);
    }

// ── Utilidades ────────────────────────────────────────────────────────────────

    private String tipoDocumentoLabel(String tipo) {
        if (tipo == null) return "";
        return switch (tipo) {
            case "FACTURA_ELECTRONICA"  -> "Factura";
            case "TIQUETE_ELECTRONICO"  -> "Tiquete";
            case "NOTA_CREDITO"         -> "Nota Crédito";
            case "NOTA_DEBITO"          -> "Nota Débito";
            default -> tipo;
        };
    }

    private void setCell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value : "");
        c.setCellStyle(style);
    }

    private void setMoney(Row row, int col, BigDecimal value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value.doubleValue() : 0.0);
        c.setCellStyle(style);
    }

    private void setMoneyTotal(Row row, int col, BigDecimal value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value.doubleValue() : 0.0);
        c.setCellStyle(style);
    }

// ── Fábrica de estilos ────────────────────────────────────────────────────────

    private CellStyle crearEstiloTitulo(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 14);
        s.setFont(f);
        return s;
    }

    private CellStyle crearEstiloSubtitulo(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 12);
        s.setFont(f);
        return s;
    }

    private CellStyle crearEstiloHeader(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)31, (byte)73, (byte)125}, null)); // azul oscuro
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(s);
        return s;
    }

    private CellStyle crearEstiloDato(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        setBorder(s);
        return s;
    }

    private CellStyle crearEstiloMoneda(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        DataFormat fmt = wb.createDataFormat();
        s.setDataFormat(fmt.getFormat("₡#,##0.00"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        setBorder(s);
        return s;
    }

    private CellStyle crearEstiloFecha(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        setBorder(s);
        return s;
    }

    private CellStyle crearEstiloTotal(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)31, (byte)73, (byte)125}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        DataFormat fmt = wb.createDataFormat();
        s.setDataFormat(fmt.getFormat("₡#,##0.00"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        setBorder(s);
        return s;
    }

    private CellStyle crearEstiloNotaCredito(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setItalic(true);
        f.setColor(new XSSFColor(new byte[]{(byte)192, (byte)0, (byte)0}, null)); // rojo
        s.setFont(f);
        setBorder(s);
        return s;
    }

    private CellStyle crearEstiloNotaCreditoMoneda(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setItalic(true);
        f.setColor(new XSSFColor(new byte[]{(byte)192, (byte)0, (byte)0}, null));
        s.setFont(f);
        DataFormat fmt = wb.createDataFormat();
        s.setDataFormat(fmt.getFormat("₡#,##0.00"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        setBorder(s);
        return s;
    }

    private void setBorder(CellStyle s) {
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }
}