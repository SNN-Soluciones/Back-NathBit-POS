package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.service.SincronizacionHaciendaService;
import com.snnsoluciones.backnathbitpos.service.SincronizacionHaciendaService.VerificacionHaciendaDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * API para verificar y reportar el estado REAL de comprobantes en Hacienda
 * 
 * Endpoints:
 * - POST /api/sincronizacion-hacienda/verificar/{id} - Verificar una factura
 * - POST /api/sincronizacion-hacienda/verificar-masivo - Verificar múltiples
 * - GET /api/sincronizacion-hacienda/reporte-excel - Generar reporte Excel
 * 
 * @author NathBit POS
 */
@Slf4j
@RestController
@RequestMapping("/api/sincronizacion-hacienda")
@RequiredArgsConstructor
@Tag(name = "Sincronización Hacienda", description = "Verificación de estados en Hacienda")
public class SincronizacionHaciendaController {

    private final SincronizacionHaciendaService sincronizacionService;

    /**
     * Verifica el estado de UNA factura específica en Hacienda
     * 
     * POST /api/sincronizacion-hacienda/verificar/629
     */
    @Operation(summary = "Verificar factura individual",
        description = "Consulta el estado REAL de una factura en Hacienda y actualiza la BD")
    @PostMapping("/verificar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<VerificacionHaciendaDTO>> verificarFactura(@PathVariable Long id) {
        log.info("📋 Solicitud de verificación para factura ID: {}", id);

        try {
            VerificacionHaciendaDTO resultado = sincronizacionService.verificarFactura(id);

            if (resultado.isSincronizado()) {
                return ResponseEntity.ok(ApiResponse.success(
                    "Factura verificada exitosamente - Estado: " + resultado.getEstadoHaciendaActual(),
                    resultado
                ));
            } else {
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .body(ApiResponse.error(resultado.getMensaje(), resultado));
            }

        } catch (Exception e) {
            log.error("Error verificando factura {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al verificar: " + e.getMessage()));
        }
    }

    /**
     * Verifica MÚLTIPLES facturas de una empresa en un rango de fechas
     * 
     * POST /api/sincronizacion-hacienda/verificar-masivo
     *   ?empresaId=2
     *   &fechaDesde=2025-11-01T00:00:00
     *   &fechaHasta=2025-12-31T23:59:59
     */
    @Operation(summary = "Verificación masiva",
        description = "Verifica todas las facturas de una empresa en un rango de fechas")
    @PostMapping("/verificar-masivo")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<List<VerificacionHaciendaDTO>>> verificarMasivo(
            @RequestParam Long empresaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta) {

        log.info("📋 Verificación masiva - Empresa: {}, Rango: {} a {}", 
            empresaId, fechaDesde, fechaHasta);

        try {
            List<VerificacionHaciendaDTO> resultados = sincronizacionService.verificarFacturasMasivo(
                empresaId, fechaDesde, fechaHasta
            );

            long exitosos = resultados.stream().filter(VerificacionHaciendaDTO::isSincronizado).count();
            long fallidos = resultados.size() - exitosos;

            String mensaje = String.format("Verificación completada - Exitosos: %d, Fallidos: %d", 
                exitosos, fallidos);

            return ResponseEntity.ok(ApiResponse.success(mensaje, resultados));

        } catch (Exception e) {
            log.error("Error en verificación masiva", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    /**
     * Genera un reporte Excel con el estado verificado de las facturas
     * 
     * GET /api/sincronizacion-hacienda/reporte-excel
     *   ?empresaId=2
     *   &fechaDesde=2025-11-01T00:00:00
     *   &fechaHasta=2025-12-31T23:59:59
     */
    @Operation(summary = "Reporte Excel de verificación",
        description = "Genera un Excel con el estado confirmado desde Hacienda")
    @GetMapping("/reporte-excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<byte[]> generarReporteExcel(
            @RequestParam Long empresaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta) {

        log.info("📊 Generando reporte Excel - Empresa: {}", empresaId);

        try {
            // 1. Verificar facturas
            List<VerificacionHaciendaDTO> resultados = sincronizacionService.verificarFacturasMasivo(
                empresaId, fechaDesde, fechaHasta
            );

            // 2. Generar Excel
            byte[] excelBytes = generarExcel(resultados, fechaDesde, fechaHasta);

            // 3. Nombre del archivo
            String filename = String.format("Verificacion_Hacienda_%s_%s.xlsx",
                fechaDesde.toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE),
                fechaHasta.toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE)
            );

            // 4. Retornar
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(excelBytes);

        } catch (Exception e) {
            log.error("Error generando reporte Excel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Genera el archivo Excel con los resultados
     */
    private byte[] generarExcel(List<VerificacionHaciendaDTO> datos, 
                                LocalDateTime fechaDesde, 
                                LocalDateTime fechaHasta) throws Exception {

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Verificación Hacienda");

            // Estilos
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            CellStyle exitoStyle = workbook.createCellStyle();
            exitoStyle.cloneStyleFrom(dataStyle);
            exitoStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            exitoStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle errorStyle = workbook.createCellStyle();
            errorStyle.cloneStyleFrom(dataStyle);
            errorStyle.setFillForegroundColor(IndexedColors.CORAL.getIndex());
            errorStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Título
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("REPORTE DE VERIFICACIÓN CON HACIENDA");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Info del reporte
            Row infoRow1 = sheet.createRow(1);
            infoRow1.createCell(0).setCellValue("Período: " + 
                fechaDesde.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + 
                " - " + 
                fechaHasta.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            Row infoRow2 = sheet.createRow(2);
            infoRow2.createCell(0).setCellValue("Generado: " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            Row infoRow3 = sheet.createRow(3);
            long exitosos = datos.stream().filter(VerificacionHaciendaDTO::isSincronizado).count();
            infoRow3.createCell(0).setCellValue("Total verificados: " + datos.size() + 
                " (Exitosos: " + exitosos + ", Fallidos: " + (datos.size() - exitosos) + ")");

            // Encabezados
            Row headerRow = sheet.createRow(5);
            String[] headers = {
                "ID", "Consecutivo", "Proveedor", "Monto", 
                "Estado Anterior", "Estado Hacienda", "Sincronizado", "Mensaje"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Datos
            int rowNum = 6;
            for (VerificacionHaciendaDTO dto : datos) {
                Row row = sheet.createRow(rowNum++);

                CellStyle rowStyle = dto.isSincronizado() ? exitoStyle : errorStyle;

                Cell cell0 = row.createCell(0);
                cell0.setCellValue(dto.getFacturaId());
                cell0.setCellStyle(rowStyle);

                Cell cell1 = row.createCell(1);
                cell1.setCellValue(dto.getNumeroConsecutivo());
                cell1.setCellStyle(rowStyle);

                Cell cell2 = row.createCell(2);
                cell2.setCellValue(dto.getProveedorNombre());
                cell2.setCellStyle(rowStyle);

                Cell cell3 = row.createCell(3);
                cell3.setCellValue(dto.getTotalComprobante().doubleValue());
                cell3.setCellStyle(rowStyle);

                Cell cell4 = row.createCell(4);
                cell4.setCellValue(dto.getEstadoInternoAnterior() != null ? dto.getEstadoInternoAnterior() : "N/A");
                cell4.setCellStyle(rowStyle);

                Cell cell5 = row.createCell(5);
                cell5.setCellValue(dto.getEstadoHaciendaActual() != null ? dto.getEstadoHaciendaActual() : "N/A");
                cell5.setCellStyle(rowStyle);

                Cell cell6 = row.createCell(6);
                cell6.setCellValue(dto.isSincronizado() ? "✓ SÍ" : "✗ NO");
                cell6.setCellStyle(rowStyle);

                Cell cell7 = row.createCell(7);
                cell7.setCellValue(dto.getMensaje());
                cell7.setCellStyle(rowStyle);
            }

            // Ajustar anchos
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}