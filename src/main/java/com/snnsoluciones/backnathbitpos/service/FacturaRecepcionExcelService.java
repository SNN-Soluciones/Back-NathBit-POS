package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.facturarecepcion.FacturaRecepcionReporteDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio para generar reportes Excel de facturas de recepción
 * Utiliza Apache POI para crear archivos .xlsx
 *
 * Arquitectura La Jachuda 🚀
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FacturaRecepcionExcelService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Genera un archivo Excel con el reporte de facturas aceptadas
     *
     * @param datos Lista de facturas a incluir en el reporte
     * @param fechaInicio Fecha inicio del rango
     * @param fechaFin Fecha fin del rango
     * @return Archivo Excel como byte array
     */
    public byte[] generarExcel(List<FacturaRecepcionReporteDTO> datos, LocalDate fechaInicio, LocalDate fechaFin) {
        log.info("Generando reporte Excel con {} facturas", datos.size());

        try (XSSFWorkbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Facturas Aceptadas");

            // Crear estilos
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);

            int rowNum = 0;

            // TÍTULO
            rowNum = createTitle(sheet, rowNum, titleStyle, fechaInicio, fechaFin);

            // HEADERS
            rowNum = createHeaders(sheet, rowNum, headerStyle);

            // DATOS
            rowNum = createDataRows(sheet, rowNum, datos, dataStyle, currencyStyle, dateStyle);

            // TOTALES
            createTotalsRow(sheet, rowNum, datos, totalStyle, currencyStyle);

            // Auto-size columnas (ahora son 25 columnas)
            autoSizeColumns(sheet, 25);

            workbook.write(out);

            log.info("Reporte Excel generado exitosamente: {} bytes", out.size());
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Error generando reporte Excel", e);
            throw new RuntimeException("Error generando reporte Excel: " + e.getMessage(), e);
        }
    }

    private int createTitle(Sheet sheet, int rowNum, CellStyle titleStyle, LocalDate fechaInicio, LocalDate fechaFin) {
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("REPORTE DE FACTURAS ACEPTADAS");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(titleRow.getRowNum(), titleRow.getRowNum(), 0, 24)); // 25 columnas = 0-24

        Row dateRow = sheet.createRow(rowNum++);
        Cell dateCell = dateRow.createCell(0);
        dateCell.setCellValue(String.format("Período: %s al %s",
            fechaInicio.format(DATE_FORMATTER),
            fechaFin.format(DATE_FORMATTER)));
        sheet.addMergedRegion(new CellRangeAddress(dateRow.getRowNum(), dateRow.getRowNum(), 0, 14));

        rowNum++; // Línea en blanco
        return rowNum;
    }

    private int createHeaders(Sheet sheet, int rowNum, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(rowNum++);

        String[] headers = {
            "Tipo", "Cédula", "Nombre Emisor", "Fecha", "Clave", "Motivo",
            "Serv. Gravados", "Serv. Exentos", "Serv. No Suj.",
            "Merc. Gravadas", "Merc. Exentas", "Merc. No Suj.",
            "Subtotal", "IVA Total",
            "IVA 0%", "IVA 1%", "IVA 2%", "IVA 4%", "IVA 8%", "IVA 13%",
            "Descuentos", "Otros Cargos", "IVA Devuelto", "Exonerado", "Total"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        return rowNum;
    }

    private int createDataRows(Sheet sheet, int rowNum, List<FacturaRecepcionReporteDTO> datos,
        CellStyle dataStyle, CellStyle currencyStyle, CellStyle dateStyle) {

        for (FacturaRecepcionReporteDTO dto : datos) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;

            // Tipo
            createCell(row, colNum++, dto.getTipoDocumento(), dataStyle);

            // Cédula
            createCell(row, colNum++, dto.getCedulaEmisor(), dataStyle);

            // Nombre Emisor
            createCell(row, colNum++, dto.getNombreEmisor(), dataStyle);

            // Fecha
            Cell dateCell = row.createCell(colNum++);
            dateCell.setCellValue(dto.getFechaEmision().format(DATETIME_FORMATTER));
            dateCell.setCellStyle(dateStyle);

            // Clave (completa - no truncar)
            createCell(row, colNum++, dto.getClave(), dataStyle);

            // Motivo Respuesta
            createCell(row, colNum++, dto.getMotivoRespuesta(), dataStyle);

            // SERVICIOS
            createCurrencyCell(row, colNum++, dto.getTotalServiciosGravados(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getTotalServiciosExentos(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getTotalServiciosNoSujetos(), currencyStyle);

            // MERCANCÍAS
            createCurrencyCell(row, colNum++, dto.getTotalMercanciasGravadas(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getTotalMercanciasExentas(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getTotalMercanciasNoSujetas(), currencyStyle);

            // TOTALES
            createCurrencyCell(row, colNum++, dto.getTotalVentaNeta(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getTotalImpuesto(), currencyStyle);

            // IVA POR TARIFA
            createCurrencyCell(row, colNum++, dto.getIva0(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getIva1(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getIva2(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getIva4(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getIva8(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getIva13(), currencyStyle);

            // OTROS TOTALES
            createCurrencyCell(row, colNum++, dto.getTotalDescuentos(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getTotalOtrosCargos(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getTotalIVADevuelto(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getTotalExonerado(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getTotalComprobante(), currencyStyle);
        }

        return rowNum;
    }

    private void createTotalsRow(Sheet sheet, int rowNum, List<FacturaRecepcionReporteDTO> datos,
        CellStyle totalStyle, CellStyle currencyStyle) {

        Row totalRow = sheet.createRow(rowNum);

        // Etiqueta
        Cell labelCell = totalRow.createCell(0);
        labelCell.setCellValue("TOTALES CONSOLIDADOS");
        labelCell.setCellStyle(totalStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 5));

        // Calcular totales con signo
        BigDecimal totalServGrav = BigDecimal.ZERO;
        BigDecimal totalServExent = BigDecimal.ZERO;
        BigDecimal totalServNoSuj = BigDecimal.ZERO;
        BigDecimal totalMercGrav = BigDecimal.ZERO;
        BigDecimal totalMercExent = BigDecimal.ZERO;
        BigDecimal totalMercNoSuj = BigDecimal.ZERO;
        BigDecimal totalSubtotal = BigDecimal.ZERO;
        BigDecimal totalIVA = BigDecimal.ZERO;

        // IVA por tarifa
        BigDecimal totalIVA0 = BigDecimal.ZERO;
        BigDecimal totalIVA1 = BigDecimal.ZERO;
        BigDecimal totalIVA2 = BigDecimal.ZERO;
        BigDecimal totalIVA4 = BigDecimal.ZERO;
        BigDecimal totalIVA8 = BigDecimal.ZERO;
        BigDecimal totalIVA13 = BigDecimal.ZERO;

        // Otros
        BigDecimal totalDescuentos = BigDecimal.ZERO;
        BigDecimal totalOtrosCargos = BigDecimal.ZERO;
        BigDecimal totalIVADevuelto = BigDecimal.ZERO;
        BigDecimal totalExonerado = BigDecimal.ZERO;
        BigDecimal totalFinal = BigDecimal.ZERO;

        for (FacturaRecepcionReporteDTO dto : datos) {
            BigDecimal signo = new BigDecimal(dto.getSigno());

            totalServGrav = totalServGrav.add(dto.getTotalServiciosGravados().multiply(signo));
            totalServExent = totalServExent.add(dto.getTotalServiciosExentos().multiply(signo));
            totalServNoSuj = totalServNoSuj.add(dto.getTotalServiciosNoSujetos().multiply(signo));
            totalMercGrav = totalMercGrav.add(dto.getTotalMercanciasGravadas().multiply(signo));
            totalMercExent = totalMercExent.add(dto.getTotalMercanciasExentas().multiply(signo));
            totalMercNoSuj = totalMercNoSuj.add(dto.getTotalMercanciasNoSujetas().multiply(signo));
            totalSubtotal = totalSubtotal.add(dto.getTotalVentaNeta().multiply(signo));
            totalIVA = totalIVA.add(dto.getTotalImpuesto().multiply(signo));

            // IVA por tarifa
            totalIVA0 = totalIVA0.add(dto.getIva0().multiply(signo));
            totalIVA1 = totalIVA1.add(dto.getIva1().multiply(signo));
            totalIVA2 = totalIVA2.add(dto.getIva2().multiply(signo));
            totalIVA4 = totalIVA4.add(dto.getIva4().multiply(signo));
            totalIVA8 = totalIVA8.add(dto.getIva8().multiply(signo));
            totalIVA13 = totalIVA13.add(dto.getIva13().multiply(signo));

            // Otros
            totalDescuentos = totalDescuentos.add(dto.getTotalDescuentos().multiply(signo));
            totalOtrosCargos = totalOtrosCargos.add(dto.getTotalOtrosCargos().multiply(signo));
            totalIVADevuelto = totalIVADevuelto.add(dto.getTotalIVADevuelto().multiply(signo));
            totalExonerado = totalExonerado.add(dto.getTotalExonerado().multiply(signo));
            totalFinal = totalFinal.add(dto.getTotalComprobante().multiply(signo));
        }

        // Crear celdas de totales
        CellStyle totalCurrencyStyle = createTotalCurrencyStyle(sheet.getWorkbook());

        int colNum = 6; // Columna 6: Primera columna de datos numéricos (Serv. Gravados)
        createCurrencyCell(totalRow, colNum++, totalServGrav, totalCurrencyStyle);
        createCurrencyCell(totalRow, colNum++, totalServExent, totalCurrencyStyle);
        createCurrencyCell(totalRow, colNum++, totalServNoSuj, totalCurrencyStyle);
        createCurrencyCell(totalRow, colNum++, totalMercGrav, totalCurrencyStyle);
        createCurrencyCell(totalRow, colNum++, totalMercExent, totalCurrencyStyle);
        createCurrencyCell(totalRow, colNum++, totalMercNoSuj, totalCurrencyStyle);
        createCurrencyCell(totalRow, colNum++, totalSubtotal, totalCurrencyStyle);
        createCurrencyCell(totalRow, colNum++, totalIVA, totalCurrencyStyle);

        // IVA por tarifa
        createCurrencyCell(totalRow, colNum++, totalIVA0, totalCurrencyStyle);
        createCurrencyCell(totalRow, colNum++, totalIVA1, totalCurrencyStyle);
        createCurrencyCell(totalRow, colNum++, totalIVA2, totalCurrencyStyle);
        createCurrencyCell(totalRow, colNum++, totalIVA4, totalCurrencyStyle);
        createCurrencyCell(totalRow, colNum++, totalIVA8, totalCurrencyStyle);
        createCurrencyCell(totalRow, colNum++, totalIVA13, totalCurrencyStyle);

        // Otros
        createCurrencyCell(totalRow, colNum++, totalDescuentos, totalCurrencyStyle);
        createCurrencyCell(totalRow, colNum++, totalOtrosCargos, totalCurrencyStyle);
        createCurrencyCell(totalRow, colNum++, totalIVADevuelto, totalCurrencyStyle);
        createCurrencyCell(totalRow, colNum++, totalExonerado, totalCurrencyStyle);
        createCurrencyCell(totalRow, colNum++, totalFinal, totalCurrencyStyle);
    }

    // ==================== HELPERS ====================

    private void createCell(Row row, int colNum, String value, CellStyle style) {
        Cell cell = row.createCell(colNum);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void createCurrencyCell(Row row, int colNum, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(colNum);
        cell.setCellValue(value != null ? value.doubleValue() : 0.0);
        cell.setCellStyle(style);
    }

    private void autoSizeColumns(Sheet sheet, int numColumns) {
        for (int i = 0; i < numColumns; i++) {
            sheet.autoSizeColumn(i);
            // Ajustar un poco más para que no quede tan apretado
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, (int) (currentWidth * 1.1));
        }
    }

    // ==================== ESTILOS ====================

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);

        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("₡#,##0.00"));

        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createTotalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);

        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderLeft(BorderStyle.MEDIUM);
        style.setBorderRight(BorderStyle.MEDIUM);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    private CellStyle createTotalCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("₡#,##0.00"));

        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderLeft(BorderStyle.MEDIUM);
        style.setBorderRight(BorderStyle.MEDIUM);

        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }
}