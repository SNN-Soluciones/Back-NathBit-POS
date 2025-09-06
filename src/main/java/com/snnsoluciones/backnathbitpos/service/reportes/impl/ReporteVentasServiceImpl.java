package com.snnsoluciones.backnathbitpos.service.reportes.impl;

import com.snnsoluciones.backnathbitpos.dto.reporte.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.reportes.ReporteVentasService;
import com.snnsoluciones.backnathbitpos.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementación del servicio de reportes de ventas
 * "Where we're going, we don't need roads" - Doc Brown
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteVentasServiceImpl implements ReporteVentasService {

    private final ReporteVentasRepository reporteVentasRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public ReporteVentasResponse generarReporteVentas(ReporteVentasRequest request)
        throws JRException, IOException {

        log.info("Generando reporte de ventas para sucursal {} desde {} hasta {}",
            request.getSucursalId(), request.getFechaDesde(), request.getFechaHasta());

        // 1. Validar acceso
        validarAccesoSucursal(request.getSucursalId());

        // 2. Obtener datos
        ReporteVentasResponse datos = obtenerDatosReporte(request);

        // 3. Generar archivo según formato
        switch (request.getFormato()) {
            case EXCEL:
                generarExcelProgramatico(datos);
                break;
            case PDF:
                generarPDFJasper(datos);
                break;
            case CSV:
                generarCSV(datos);
                break;
        }

        return datos;
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteVentasResponse obtenerDatosReporte(ReporteVentasRequest request) {
        // Convertir fechas a DateTime (inicio y fin del día)
        LocalDateTime fechaDesde = request.getFechaDesde().atStartOfDay();
        LocalDateTime fechaHasta = request.getFechaHasta().atTime(23, 59, 59);

        // Obtener datos de la sucursal
        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
            .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada"));

        // Obtener líneas del reporte
        List<ReporteVentasLineaDTO> lineas = reporteVentasRepository
            .obtenerDatosReporteVentas(request.getSucursalId(), fechaDesde, fechaHasta);

        // Ajustar montos para notas de crédito
        lineas.forEach(ReporteVentasLineaDTO::ajustarParaNotaCredito);

        // Construir response
        ReporteVentasResponse response = ReporteVentasResponse.builder()
            .empresaNombre(sucursal.getEmpresa().getNombreRazonSocial())
            .empresaIdentificacion(sucursal.getEmpresa().getIdentificacion())
            .sucursalNombre(sucursal.getNombre())
            .fechaDesde(request.getFechaDesde())
            .fechaHasta(request.getFechaHasta())
            .fechaGeneracion(LocalDateTime.now())
            .generadoPor(SecurityUtils.getCurrentUserLogin())
            .lineas(lineas)
            .build();

        // Calcular totales
        response.calcularTotales();

        return response;
    }

    @Override
    public void validarAccesoSucursal(Long sucursalId) {
        // TODO: Implementar validación de permisos
        // Por ahora solo validamos que exista
        if (!sucursalRepository.existsById(sucursalId)) {
            throw new EntityNotFoundException("Sucursal no encontrada: " + sucursalId);
        }
    }

    /**
     * Genera Excel usando Apache POI (más control)
     */
    private void generarExcelProgramatico(ReporteVentasResponse datos) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Ventas");

            // Estilos
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle moneyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            moneyStyle.setDataFormat(format.getFormat("₡#,##0.00"));

            int rowNum = 0;

            // Título
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("REPORTE DE VENTAS");

            // Información de encabezado
            rowNum++; // Línea en blanco
            createInfoRow(sheet, rowNum++, "Empresa:", datos.getEmpresaNombre());
            createInfoRow(sheet, rowNum++, "Cédula:", datos.getEmpresaIdentificacion());
            createInfoRow(sheet, rowNum++, "Sucursal:", datos.getSucursalNombre());
            createInfoRow(sheet, rowNum++, "Período:",
                datos.getFechaDesde() + " al " + datos.getFechaHasta());
            createInfoRow(sheet, rowNum++, "Generado:", datos.getFechaGeneracion().toString());

            rowNum++; // Línea en blanco

            // Headers de columnas
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {
                "Clave", "Consecutivo", "Tipo Doc", "Fecha", "Actividad",
                "Cliente", "Cédula", "Mercancías Gravadas", "Mercancías Exentas",
                "Mercancías Exoneradas", "Servicios Gravados", "Servicios Exentos",
                "Servicios Exonerados", "Subtotal Gravado", "Subtotal Exento",
                "Subtotal Exonerado", "Venta Neta", "Impuestos", "Descuentos",
                "Exonerado", "Total Comprobante"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Datos
            for (ReporteVentasLineaDTO linea : datos.getLineas()) {
                Row row = sheet.createRow(rowNum++);
                int colNum = 0;

                // Datos textuales
                row.createCell(colNum++).setCellValue(linea.getClave());
                row.createCell(colNum++).setCellValue(linea.getConsecutivo());
                row.createCell(colNum++).setCellValue(linea.getTipoDocumento());
                row.createCell(colNum++).setCellValue(linea.getFechaEmision().toString());
                row.createCell(colNum++).setCellValue(
                    linea.getActividadEconomicaCodigo() + " - " +
                        linea.getActividadEconomicaDescripcion()
                );
                row.createCell(colNum++).setCellValue(linea.getClienteNombre());
                row.createCell(colNum++).setCellValue(linea.getClienteIdentificacion());

                // Datos numéricos con estilo
                createMoneyCell(row, colNum++, linea.getTotalMercanciasGravadas(), moneyStyle);
                createMoneyCell(row, colNum++, linea.getTotalMercanciasExentas(), moneyStyle);
                createMoneyCell(row, colNum++, linea.getTotalMercanciasExoneradas(), moneyStyle);
                createMoneyCell(row, colNum++, linea.getTotalServiciosGravados(), moneyStyle);
                createMoneyCell(row, colNum++, linea.getTotalServiciosExentos(), moneyStyle);
                createMoneyCell(row, colNum++, linea.getTotalServiciosExonerados(), moneyStyle);
                createMoneyCell(row, colNum++, linea.getSubtotalGravado(), moneyStyle);
                createMoneyCell(row, colNum++, linea.getSubtotalExento(), moneyStyle);
                createMoneyCell(row, colNum++, linea.getSubtotalExonerado(), moneyStyle);
                createMoneyCell(row, colNum++, linea.getTotalVentaNeta(), moneyStyle);
                createMoneyCell(row, colNum++, linea.getTotalImpuesto(), moneyStyle);
                createMoneyCell(row, colNum++, linea.getTotalDescuentos(), moneyStyle);
                createMoneyCell(row, colNum++, linea.getMontoTotalExonerado(), moneyStyle);
                createMoneyCell(row, colNum++, linea.getTotalComprobante(), moneyStyle);
            }

            // Totales
            rowNum++; // Línea en blanco
            Row totalRow = sheet.createRow(rowNum);
            totalRow.createCell(6).setCellValue("TOTALES:");
            createMoneyCell(totalRow, 7, datos.getTotalMercanciasGravadas(), moneyStyle);
            createMoneyCell(totalRow, 8, datos.getTotalMercanciasExentas(), moneyStyle);
            createMoneyCell(totalRow, 9, datos.getTotalMercanciasExoneradas(), moneyStyle);
            createMoneyCell(totalRow, 10, datos.getTotalServiciosGravados(), moneyStyle);
            createMoneyCell(totalRow, 11, datos.getTotalServiciosExentos(), moneyStyle);
            createMoneyCell(totalRow, 12, datos.getTotalServiciosExonerados(), moneyStyle);
            createMoneyCell(totalRow, 13, datos.getSubtotalGravado(), moneyStyle);
            createMoneyCell(totalRow, 14, datos.getSubtotalExento(), moneyStyle);
            createMoneyCell(totalRow, 15, datos.getSubtotalExonerado(), moneyStyle);
            createMoneyCell(totalRow, 16, datos.getTotalVentaNeta(), moneyStyle);
            createMoneyCell(totalRow, 17, datos.getTotalImpuestos(), moneyStyle);
            createMoneyCell(totalRow, 18, datos.getTotalDescuentos(), moneyStyle);
            createMoneyCell(totalRow, 19, datos.getTotalExonerado(), moneyStyle);
            createMoneyCell(totalRow, 20, datos.getTotalGeneral(), moneyStyle);

            // Autoajustar columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Guardar en ByteArray
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);

            datos.setArchivoGenerado(outputStream.toByteArray());
            datos.setNombreArchivo("ventas_" + datos.getFechaDesde() + "_" +
                datos.getFechaHasta() + ".xlsx");
            datos.setTipoContenido("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }
    }

    private void createInfoRow(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }

    private void createMoneyCell(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
            cell.setCellStyle(style);
        } else {
            cell.setCellValue(0);
            cell.setCellStyle(style);
        }
    }

    /**
     * Genera PDF usando JasperReports
     */
    private void generarPDFJasper(ReporteVentasResponse datos) throws JRException, IOException {
        // Por implementar con template .jrxml
        log.warn("Generación PDF pendiente de implementar");
        throw new UnsupportedOperationException("PDF no implementado aún");
    }

    /**
     * Genera CSV simple
     */
    private void generarCSV(ReporteVentasResponse datos) throws IOException {
        StringBuilder csv = new StringBuilder();

        // Headers
        csv.append("Clave,Consecutivo,Tipo Doc,Fecha,Actividad,Cliente,Cedula,");
        csv.append("Merc.Gravadas,Merc.Exentas,Merc.Exoneradas,");
        csv.append("Serv.Gravados,Serv.Exentos,Serv.Exonerados,");
        csv.append("Sub.Gravado,Sub.Exento,Sub.Exonerado,");
        csv.append("Venta Neta,Impuestos,Descuentos,Exonerado,Total\n");

        // Datos
        for (ReporteVentasLineaDTO linea : datos.getLineas()) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,",
                linea.getClave(),
                linea.getConsecutivo(),
                linea.getTipoDocumento(),
                linea.getFechaEmision(),
                linea.getActividadEconomicaCodigo(),
                linea.getClienteNombre(),
                linea.getClienteIdentificacion()
            ));

            csv.append(String.format("%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,",
                toBigDecimalSafe(linea.getTotalMercanciasGravadas()),
                toBigDecimalSafe(linea.getTotalMercanciasExentas()),
                toBigDecimalSafe(linea.getTotalMercanciasExoneradas()),
                toBigDecimalSafe(linea.getTotalServiciosGravados()),
                toBigDecimalSafe(linea.getTotalServiciosExentos()),
                toBigDecimalSafe(linea.getTotalServiciosExonerados())
            ));

            csv.append(String.format("%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f\n",
                toBigDecimalSafe(linea.getSubtotalGravado()),
                toBigDecimalSafe(linea.getSubtotalExento()),
                toBigDecimalSafe(linea.getSubtotalExonerado()),
                toBigDecimalSafe(linea.getTotalVentaNeta()),
                toBigDecimalSafe(linea.getTotalImpuesto()),
                toBigDecimalSafe(linea.getTotalDescuentos()),
                toBigDecimalSafe(linea.getMontoTotalExonerado()),
                toBigDecimalSafe(linea.getTotalComprobante())
            ));
        }

        datos.setArchivoGenerado(csv.toString().getBytes());
        datos.setNombreArchivo("ventas_" + datos.getFechaDesde() + "_" +
            datos.getFechaHasta() + ".csv");
        datos.setTipoContenido("text/csv");
    }

    private double toBigDecimalSafe(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }
}