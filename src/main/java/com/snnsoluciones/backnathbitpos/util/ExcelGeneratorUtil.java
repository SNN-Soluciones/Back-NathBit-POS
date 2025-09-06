package com.snnsoluciones.backnathbitpos.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Utilidad para generar archivos Excel de forma programática
 * "Roads? Where we're going, we don't need roads." - Doc Brown
 * Arquitectura La Jachuda 🚀
 */
@Slf4j
public class ExcelGeneratorUtil {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    /**
     * Crea un nuevo workbook con estilos predefinidos
     */
    public static XSSFWorkbook createWorkbook() {
        return new XSSFWorkbook();
    }
    
    /**
     * Crea los estilos estándar para reportes
     * @return Mapa con los estilos
     */
    public static Map<String, CellStyle> createStandardStyles(Workbook workbook) {
        Map<String, CellStyle> styles = new java.util.HashMap<>();
        
        // Estilo para título principal
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        styles.put("title", titleStyle);
        
        // Estilo para headers
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        styles.put("header", headerStyle);
        
        // Estilo para moneda (colones)
        CellStyle moneyStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        moneyStyle.setDataFormat(format.getFormat("₡#,##0.00"));
        moneyStyle.setBorderBottom(BorderStyle.THIN);
        moneyStyle.setBorderTop(BorderStyle.THIN);
        moneyStyle.setBorderLeft(BorderStyle.THIN);
        moneyStyle.setBorderRight(BorderStyle.THIN);
        styles.put("money", moneyStyle);
        
        // Estilo para moneda USD
        CellStyle dollarStyle = workbook.createCellStyle();
        dollarStyle.setDataFormat(format.getFormat("$#,##0.00"));
        dollarStyle.setBorderBottom(BorderStyle.THIN);
        dollarStyle.setBorderTop(BorderStyle.THIN);
        dollarStyle.setBorderLeft(BorderStyle.THIN);
        dollarStyle.setBorderRight(BorderStyle.THIN);
        styles.put("dollar", dollarStyle);
        
        // Estilo para porcentajes
        CellStyle percentStyle = workbook.createCellStyle();
        percentStyle.setDataFormat(format.getFormat("0.00%"));
        percentStyle.setBorderBottom(BorderStyle.THIN);
        percentStyle.setBorderTop(BorderStyle.THIN);
        percentStyle.setBorderLeft(BorderStyle.THIN);
        percentStyle.setBorderRight(BorderStyle.THIN);
        styles.put("percent", percentStyle);
        
        // Estilo para fechas
        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(format.getFormat("dd/mm/yyyy"));
        dateStyle.setBorderBottom(BorderStyle.THIN);
        dateStyle.setBorderTop(BorderStyle.THIN);
        dateStyle.setBorderLeft(BorderStyle.THIN);
        dateStyle.setBorderRight(BorderStyle.THIN);
        styles.put("date", dateStyle);
        
        // Estilo para celdas normales con bordes
        CellStyle normalStyle = workbook.createCellStyle();
        normalStyle.setBorderBottom(BorderStyle.THIN);
        normalStyle.setBorderTop(BorderStyle.THIN);
        normalStyle.setBorderLeft(BorderStyle.THIN);
        normalStyle.setBorderRight(BorderStyle.THIN);
        styles.put("normal", normalStyle);
        
        // Estilo para totales
        CellStyle totalStyle = workbook.createCellStyle();
        Font totalFont = workbook.createFont();
        totalFont.setBold(true);
        totalStyle.setFont(totalFont);
        totalStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalStyle.setBorderBottom(BorderStyle.DOUBLE);
        totalStyle.setBorderTop(BorderStyle.THIN);
        totalStyle.setBorderLeft(BorderStyle.THIN);
        totalStyle.setBorderRight(BorderStyle.THIN);
        styles.put("total", totalStyle);
        
        return styles;
    }
    
    /**
     * Crea una fila de información (label: valor)
     */
    public static void createInfoRow(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value != null ? value : "");
    }
    
    /**
     * Crea una fila de headers con estilo
     */
    public static void createHeaderRow(Sheet sheet, int rowNum, String[] headers, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(rowNum);
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            if (headerStyle != null) {
                cell.setCellStyle(headerStyle);
            }
        }
    }
    
    /**
     * Crea una celda con valor monetario
     */
    public static void createMoneyCell(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue(0);
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }
    
    /**
     * Crea una celda con porcentaje
     */
    public static void createPercentCell(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            // Dividir entre 100 porque Excel espera 0.15 para 15%
            cell.setCellValue(value.doubleValue() / 100.0);
        } else {
            cell.setCellValue(0);
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }
    
    /**
     * Crea una celda con fecha
     */
    public static void createDateCell(Row row, int column, LocalDate date, CellStyle style) {
        Cell cell = row.createCell(column);
        if (date != null) {
            cell.setCellValue(date.format(DATE_FORMATTER));
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }
    
    /**
     * Crea una celda con fecha y hora
     */
    public static void createDateTimeCell(Row row, int column, LocalDateTime dateTime, CellStyle style) {
        Cell cell = row.createCell(column);
        if (dateTime != null) {
            cell.setCellValue(dateTime.format(DATETIME_FORMATTER));
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }
    
    /**
     * Auto-ajusta el ancho de las columnas
     */
    public static void autoSizeColumns(Sheet sheet, int numberOfColumns) {
        for (int i = 0; i < numberOfColumns; i++) {
            sheet.autoSizeColumn(i);
            // Agregar un poco de espacio extra
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, currentWidth + 256);
        }
    }
    
    /**
     * Agrega un título principal con merge de celdas
     */
    public static void addMainTitle(Sheet sheet, String title, int numberOfColumns, CellStyle titleStyle) {
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        if (titleStyle != null) {
            titleCell.setCellStyle(titleStyle);
        }
        
        // Merge celdas para el título
        if (numberOfColumns > 1) {
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, numberOfColumns - 1));
        }
    }
    
    /**
     * Convierte el workbook a byte array
     */
    public static byte[] workbookToByteArray(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
    
    /**
     * Agrega filtros automáticos a los headers
     */
    public static void addAutoFilter(Sheet sheet, int headerRow, int lastColumn) {
        sheet.setAutoFilter(new CellRangeAddress(headerRow, sheet.getLastRowNum(), 0, lastColumn));
    }
    
    /**
     * Congela paneles (freeze panes) para mantener headers visibles
     */
    public static void freezePanes(Sheet sheet, int colSplit, int rowSplit) {
        sheet.createFreezePane(colSplit, rowSplit);
    }
    
    /**
     * Crea una hoja con formato estándar de reporte
     */
    public static Sheet createReportSheet(Workbook workbook, String sheetName, 
            String reportTitle, Map<String, String> headerInfo, String[] columnHeaders) {
        
        Sheet sheet = workbook.createSheet(sheetName);
        Map<String, CellStyle> styles = createStandardStyles(workbook);
        
        int currentRow = 0;
        
        // Título principal
        addMainTitle(sheet, reportTitle, columnHeaders.length, styles.get("title"));
        currentRow += 2;
        
        // Información del encabezado
        for (Map.Entry<String, String> entry : headerInfo.entrySet()) {
            createInfoRow(sheet, currentRow++, entry.getKey(), entry.getValue());
        }
        currentRow++; // Línea en blanco
        
        // Headers de columnas
        createHeaderRow(sheet, currentRow, columnHeaders, styles.get("header"));
        
        // Congelar paneles para mantener headers visibles
        freezePanes(sheet, 0, currentRow + 1);
        
        return sheet;
    }
    
    /**
     * Helper para valores seguros de BigDecimal
     */
    public static double safeDoubleValue(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }
    
    /**
     * Helper para strings seguros
     */
    public static String safeStringValue(String value) {
        return value != null ? value : "";
    }
    
    /**
     * Agrega bordes a un rango de celdas
     */
    public static void addBordersToRange(Sheet sheet, int firstRow, int lastRow, 
            int firstCol, int lastCol, BorderStyle borderStyle) {
        
        for (int row = firstRow; row <= lastRow; row++) {
            Row currentRow = sheet.getRow(row);
            if (currentRow == null) {
                currentRow = sheet.createRow(row);
            }
            
            for (int col = firstCol; col <= lastCol; col++) {
                Cell cell = currentRow.getCell(col);
                if (cell == null) {
                    cell = currentRow.createCell(col);
                }
                
                CellStyle style = sheet.getWorkbook().createCellStyle();
                style.cloneStyleFrom(cell.getCellStyle());
                style.setBorderBottom(borderStyle);
                style.setBorderTop(borderStyle);
                style.setBorderLeft(borderStyle);
                style.setBorderRight(borderStyle);
                cell.setCellStyle(style);
            }
        }
    }
}