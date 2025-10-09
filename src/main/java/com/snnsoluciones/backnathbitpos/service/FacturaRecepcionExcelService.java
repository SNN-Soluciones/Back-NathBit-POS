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

            // Auto-size columnas (ahora son 27 columnas)
            autoSizeColumns(sheet, 27);

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
            // Identificación (6)
            "Tipo", "Cédula", "Nombre Emisor", "Fecha", "Clave", "Tipo de Compra",

            // Impuestos (8)
            "IVA 0%", "IVA 1%", "IVA 2%", "IVA 4%", "IVA 8%", "IVA 13%",
            "IVA Total", "Otros Impuestos",

            // Servicios (3)
            "Serv. Gravados", "Serv. Exentos", "Serv. No Suj.",

            // Mercancías (3)
            "Merc. Gravadas", "Merc. Exentas", "Merc. No Suj.",

            // Subtotal y totales (7)
            "Subtotal", "Impuestos Totales", "Descuentos", "Otros Cargos",
            "IVA Devuelto", "Exonerado", "Total"
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

            // IDENTIFICACIÓN (6 columnas)
            createCell(row, colNum++, dto.getTipoDocumento(), dataStyle);
            createCell(row, colNum++, dto.getCedulaEmisor(), dataStyle);
            createCell(row, colNum++, dto.getNombreEmisor(), dataStyle);

            Cell dateCell = row.createCell(colNum++);
            dateCell.setCellValue(dto.getFechaEmision().format(DATETIME_FORMATTER));
            dateCell.setCellStyle(dateStyle);

            createCell(row, colNum++, dto.getClave(), dataStyle);
            createCell(row, colNum++, dto.getTipoCompra(), dataStyle);  // ← CAMBIO AQUÍ

            // IMPUESTOS (8 columnas)
            createCurrencyCell(row, colNum++, dto.getIva0(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getIva1(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getIva2(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getIva4(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getIva8(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getIva13(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getTotalSoloIVA(), currencyStyle);  // ← CALCULADO
            createCurrencyCell(row, colNum++, dto.getOtrosImpuestos(), currencyStyle);  // ← NUEVO

            // SERVICIOS (3 columnas)
            createCurrencyCell(row, colNum++, dto.getTotalServiciosGravados(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getTotalServiciosExentos(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getTotalServiciosNoSujetos(), currencyStyle);

            // MERCANCÍAS (3 columnas)
            createCurrencyCell(row, colNum++, dto.getTotalMercanciasGravadas(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getTotalMercanciasExentas(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getTotalMercanciasNoSujetas(), currencyStyle);

            // SUBTOTAL Y TOTALES (7 columnas)
            createCurrencyCell(row, colNum++, dto.getTotalVentaNeta(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getTotalTodosImpuestos(), currencyStyle);  // ← CALCULADO
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
        Cell labelCell = totalRow.createCell(0);
        labelCell.setCellValue("TOTALES");
        labelCell.setCellStyle(totalStyle);

        // Merge primeras 6 columnas para "TOTALES"
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 5));

        int colNum = 6;

        // IVA 0%
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaRecepcionReporteDTO::getIva0), currencyStyle);
        // IVA 1%
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaRecepcionReporteDTO::getIva1), currencyStyle);
        // IVA 2%
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaRecepcionReporteDTO::getIva2), currencyStyle);
        // IVA 4%
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaRecepcionReporteDTO::getIva4), currencyStyle);
        // IVA 8%
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaRecepcionReporteDTO::getIva8), currencyStyle);
        // IVA 13%
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaRecepcionReporteDTO::getIva13), currencyStyle);
        // IVA Total
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaRecepcionReporteDTO::getTotalSoloIVA), currencyStyle);
        // Otros Impuestos
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaRecepcionReporteDTO::getOtrosImpuestos), currencyStyle);

        // Servicios
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaRecepcionReporteDTO::getTotalServiciosGravados), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaRecepcionReporteDTO::getTotalServiciosExentos), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaRecepcionReporteDTO::getTotalServiciosNoSujetos), currencyStyle);

        // Mercancías
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaRecepcionReporteDTO::getTotalMercanciasGravadas), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaRecepcionReporteDTO::getTotalMercanciasExentas), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaRecepcionReporteDTO::getTotalMercanciasNoSujetas), currencyStyle);

        // Subtotal y totales
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaRecepcionReporteDTO::getTotalVentaNeta), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaRecepcionReporteDTO::getTotalTodosImpuestos), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaRecepcionReporteDTO::getTotalDescuentos), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaRecepcionReporteDTO::getTotalOtrosCargos), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaRecepcionReporteDTO::getTotalIVADevuelto), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaRecepcionReporteDTO::getTotalExonerado), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaRecepcionReporteDTO::getTotalComprobante), currencyStyle);
    }

    // ==================== HELPERS ====================

    private BigDecimal sumar(List<FacturaRecepcionReporteDTO> datos,
        java.util.function.Function<FacturaRecepcionReporteDTO, BigDecimal> getter) {
        return datos.stream()
            .map(getter)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

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