package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.factura.FacturaVentaReporteDTO;
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
 * Servicio para generar reportes Excel de ventas para Hacienda
 * Arquitectura La Jachuda 🚀
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FacturaVentaExcelService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generarExcel(List<FacturaVentaReporteDTO> datos, LocalDate fechaInicio, LocalDate fechaFin) {
        log.info("✅ Generando reporte Excel de ventas con {} facturas", datos.size());

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Ventas para Hacienda");

            // Estilos
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

            // Auto-size columnas (27 columnas)
            autoSizeColumns(sheet, 27);

            workbook.write(out);

            log.info("✅ Reporte Excel generado exitosamente: {} bytes", out.size());
            return out.toByteArray();

        } catch (IOException e) {
            log.error("❌ Error generando reporte Excel", e);
            throw new RuntimeException("Error generando reporte Excel: " + e.getMessage(), e);
        }
    }

    private int createTitle(Sheet sheet, int rowNum, CellStyle titleStyle, LocalDate fechaInicio, LocalDate fechaFin) {
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("REPORTE DE VENTAS PARA HACIENDA");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(titleRow.getRowNum(), titleRow.getRowNum(), 0, 26));

        Row dateRow = sheet.createRow(rowNum++);
        Cell dateCell = dateRow.createCell(0);
        dateCell.setCellValue(String.format("Período: %s al %s",
            fechaInicio.format(DATE_FORMATTER),
            fechaFin.format(DATE_FORMATTER)));
        sheet.addMergedRegion(new CellRangeAddress(dateRow.getRowNum(), dateRow.getRowNum(), 0, 14));

        rowNum++;
        return rowNum;
    }

    private int createHeaders(Sheet sheet, int rowNum, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(rowNum++);

        String[] headers = {
            // Identificación (6)
            "Tipo", "Cédula", "Nombre Cliente", "Fecha", "Clave", "Consecutivo",
            
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

    private int createDataRows(Sheet sheet, int rowNum, List<FacturaVentaReporteDTO> datos,
        CellStyle dataStyle, CellStyle currencyStyle, CellStyle dateStyle) {

        for (FacturaVentaReporteDTO dto : datos) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;

            // IDENTIFICACIÓN (6 columnas)
            createCell(row, colNum++, dto.getTipoDocumento(), dataStyle);
            createCell(row, colNum++, dto.getCedulaCliente(), dataStyle);
            createCell(row, colNum++, dto.getNombreCliente(), dataStyle);
            
            Cell dateCell = row.createCell(colNum++);
            dateCell.setCellValue(dto.getFechaEmision().format(DATETIME_FORMATTER));
            dateCell.setCellStyle(dateStyle);
            
            createCell(row, colNum++, dto.getClave(), dataStyle);
            createCell(row, colNum++, dto.getConsecutivo(), dataStyle);

            // IMPUESTOS (8 columnas)
            createCurrencyCell(row, colNum++, dto.getIva0(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getIva1(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getIva2(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getIva4(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getIva8(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getIva13(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getTotalSoloIVA(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getOtrosImpuestos(), currencyStyle);

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
            createCurrencyCell(row, colNum++, dto.getTotalTodosImpuestos(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getTotalDescuentos(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getTotalOtrosCargos(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getTotalIVADevuelto(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getTotalExonerado(), currencyStyle);
            createCurrencyCell(row, colNum++, dto.getTotalComprobante(), currencyStyle);
        }

        return rowNum;
    }

    private void createTotalsRow(Sheet sheet, int rowNum, List<FacturaVentaReporteDTO> datos,
        CellStyle totalStyle, CellStyle currencyStyle) {

        Row totalRow = sheet.createRow(rowNum);
        Cell labelCell = totalRow.createCell(0);
        labelCell.setCellValue("TOTALES");
        labelCell.setCellStyle(totalStyle);

        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 5));

        int colNum = 6;

        // Impuestos
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaVentaReporteDTO::getIva0), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaVentaReporteDTO::getIva1), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaVentaReporteDTO::getIva2), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaVentaReporteDTO::getIva4), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaVentaReporteDTO::getIva8), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaVentaReporteDTO::getIva13), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaVentaReporteDTO::getTotalSoloIVA), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaVentaReporteDTO::getOtrosImpuestos), currencyStyle);

        // Servicios
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaVentaReporteDTO::getTotalServiciosGravados), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaVentaReporteDTO::getTotalServiciosExentos), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaVentaReporteDTO::getTotalServiciosNoSujetos), currencyStyle);

        // Mercancías
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaVentaReporteDTO::getTotalMercanciasGravadas), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaVentaReporteDTO::getTotalMercanciasExentas), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaVentaReporteDTO::getTotalMercanciasNoSujetas), currencyStyle);

        // Subtotal y totales
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaVentaReporteDTO::getTotalVentaNeta), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaVentaReporteDTO::getTotalTodosImpuestos), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaVentaReporteDTO::getTotalDescuentos), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaVentaReporteDTO::getTotalOtrosCargos), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaVentaReporteDTO::getTotalIVADevuelto), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaVentaReporteDTO::getTotalExonerado), currencyStyle);
        createCurrencyCell(totalRow, colNum++, sumar(datos, FacturaVentaReporteDTO::getTotalComprobante), currencyStyle);
    }

    // MÉTODOS DE ESTILO (idénticos al de recepción)
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("₡#,##0.00"));
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createTotalStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    // HELPERS
    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void createCurrencyCell(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value.doubleValue() : 0.0);
        cell.setCellStyle(style);
    }

    private BigDecimal sumar(List<FacturaVentaReporteDTO> datos, 
                             java.util.function.Function<FacturaVentaReporteDTO, BigDecimal> getter) {
        return datos.stream()
            .map(getter)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void autoSizeColumns(Sheet sheet, int numColumns) {
        for (int i = 0; i < numColumns; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}