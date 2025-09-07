package com.snnsoluciones.backnathbitpos.service.reportes.impl;

import com.snnsoluciones.backnathbitpos.dto.reporte.*;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.mappers.ReporteVentasRowMapper;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.security.SecurityUtils;
import com.snnsoluciones.backnathbitpos.service.reportes.ReporteVentasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteVentasServiceImpl implements ReporteVentasService {

    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ReporteVentasRowMapper rowMapper;

    @Override
    @Transactional(readOnly = true)
    public ReporteVentasResponse generarReporteVentas(ReporteVentasRequest request)
        throws JRException, IOException {

        log.info("Generando reporte de ventas para sucursal {} desde {} hasta {}",
            request.getSucursalId(), request.getFechaDesde(), request.getFechaHasta());

        validarAccesoSucursal(request.getSucursalId());

        ReporteVentasResponse datos = obtenerDatosReporte(request);

        switch (request.getFormato()) {
            case EXCEL -> generarExcelProgramatico(datos);
            case PDF -> generarPDFJasper(datos);
            case CSV -> generarCSV(datos);
        }

        return datos;
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteVentasResponse obtenerDatosReporte(ReporteVentasRequest request) {
        // Fechas en formato string ISO (porque fecha_emision es varchar)
        String fechaDesde = request.getFechaDesde().atStartOfDay().toString();
        String fechaHasta = request.getFechaHasta().atTime(23,59,59).toString();

        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
            .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada"));

        String sql = """
            SELECT\s
                f.clave,
                f.consecutivo,
                f.tipo_documento AS tipoDocumento,
                f.fecha_emision AS fechaEmision,
                ae.codigo AS actividadEconomicaCodigo,
                ae.descripcion AS actividadEconomicaDescripcion,
                COALESCE(c.razon_social, 'CLIENTE GENERICO') AS clienteNombre,
                COALESCE(c.numero_identificacion, '000000000') AS clienteIdentificacion,
                COALESCE(c.tipo_identificacion, 'OTRO') AS clienteTipoIdentificacion,
                f.total_mercancias_gravadas AS totalMercanciasGravadas,
                f.total_mercancias_exentas AS totalMercanciasExentas,
                f.total_mercancias_exoneradas AS totalMercanciasExoneradas,
                f.total_servicios_gravados AS totalServiciosGravados,
                f.total_servicios_exentos AS totalServiciosExentos,
                f.total_servicios_exonerados AS totalServiciosExonerados,
                f.total_venta_neta AS totalVentaNeta,
                f.total_impuesto AS totalImpuesto,
                f.total_descuentos AS totalDescuentos,
                COALESCE(f.total_exonerado, 0) AS montoTotalExonerado,
                f.total_otros_cargos AS totalOtrosCargos,
                f.total_comprobante AS totalComprobante,
                f.codigo_moneda AS moneda,
                f.tipo_cambio AS tipoCambio,
                f.estado
            FROM facturas f
            INNER JOIN sucursales s ON f.sucursal_id = s.id
            LEFT JOIN empresa_actividades ea ON ea.empresa_id = s.empresa_id AND ea.es_principal = true
            LEFT JOIN actividades_economicas ae ON ea.actividad_id = ae.id
            LEFT JOIN clientes c ON f.cliente_id = c.id
            WHERE f.sucursal_id = ?
              AND f.fecha_emision >= ?
              AND f.fecha_emision <= ?
              AND f.estado IN ('ACEPTADA', 'GENERADA')
              AND f.tipo_documento IN ('FACTURA_ELECTRONICA', 'TIQUETE_ELECTRONICO', 'NOTA_CREDITO')
            ORDER BY f.fecha_emision ASC, f.consecutivo ASC
            """;

        List<ReporteVentasLineaDTO> lineas =
            jdbcTemplate.query(sql, rowMapper, request.getSucursalId(), fechaDesde, fechaHasta);

        lineas.forEach(ReporteVentasLineaDTO::ajustarParaNotaCredito);

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

        response.calcularTotales();
        return response;
    }

    @Override
    public void validarAccesoSucursal(Long sucursalId) {
        if (!sucursalRepository.existsById(sucursalId)) {
            throw new EntityNotFoundException("Sucursal no encontrada: " + sucursalId);
        }
    }

    // ======== Generadores ========
    private void generarExcelProgramatico(ReporteVentasResponse datos) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Ventas");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle moneyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            moneyStyle.setDataFormat(format.getFormat("₡#,##0.00"));

            int rowNum = 0;
            Row titleRow = sheet.createRow(rowNum++);
            titleRow.createCell(0).setCellValue("REPORTE DE VENTAS");

            rowNum++;
            createInfoRow(sheet, rowNum++, "Empresa:", datos.getEmpresaNombre());
            createInfoRow(sheet, rowNum++, "Cédula:", datos.getEmpresaIdentificacion());
            createInfoRow(sheet, rowNum++, "Sucursal:", datos.getSucursalNombre());
            createInfoRow(sheet, rowNum++, "Período:",
                datos.getFechaDesde() + " al " + datos.getFechaHasta());
            createInfoRow(sheet, rowNum++, "Generado:", datos.getFechaGeneracion().toString());

            rowNum++;
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

            for (ReporteVentasLineaDTO linea : datos.getLineas()) {
                Row row = sheet.createRow(rowNum++);
                int col = 0;
                row.createCell(col++).setCellValue(linea.getClave());
                row.createCell(col++).setCellValue(linea.getConsecutivo());
                row.createCell(col++).setCellValue(linea.getTipoDocumento());
                row.createCell(col++).setCellValue(linea.getFechaEmision().toString());
                row.createCell(col++).setCellValue(
                    linea.getActividadEconomicaCodigo() + " - " + linea.getActividadEconomicaDescripcion());
                row.createCell(col++).setCellValue(linea.getClienteNombre());
                row.createCell(col++).setCellValue(linea.getClienteIdentificacion());
                createMoneyCell(row, col++, linea.getTotalMercanciasGravadas(), moneyStyle);
                createMoneyCell(row, col++, linea.getTotalMercanciasExentas(), moneyStyle);
                createMoneyCell(row, col++, linea.getTotalMercanciasExoneradas(), moneyStyle);
                createMoneyCell(row, col++, linea.getTotalServiciosGravados(), moneyStyle);
                createMoneyCell(row, col++, linea.getTotalServiciosExentos(), moneyStyle);
                createMoneyCell(row, col++, linea.getTotalServiciosExonerados(), moneyStyle);
                createMoneyCell(row, col++, linea.getSubtotalGravado(), moneyStyle);
                createMoneyCell(row, col++, linea.getSubtotalExento(), moneyStyle);
                createMoneyCell(row, col++, linea.getSubtotalExonerado(), moneyStyle);
                createMoneyCell(row, col++, linea.getTotalVentaNeta(), moneyStyle);
                createMoneyCell(row, col++, linea.getTotalImpuesto(), moneyStyle);
                createMoneyCell(row, col++, linea.getTotalDescuentos(), moneyStyle);
                createMoneyCell(row, col++, linea.getMontoTotalExonerado(), moneyStyle);
                createMoneyCell(row, col++, linea.getTotalComprobante(), moneyStyle);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            datos.setArchivoGenerado(out.toByteArray());
            datos.setNombreArchivo("ventas_" + datos.getFechaDesde() + "_" + datos.getFechaHasta() + ".xlsx");
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
        cell.setCellValue(value != null ? value.doubleValue() : 0);
        cell.setCellStyle(style);
    }

    private void generarPDFJasper(ReporteVentasResponse datos) throws JRException, IOException {
        throw new UnsupportedOperationException("PDF no implementado aún");
    }

    private void generarCSV(ReporteVentasResponse datos) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("Clave,Consecutivo,Tipo Doc,Fecha,Actividad,Cliente,Cedula,")
            .append("Merc.Gravadas,Merc.Exentas,Merc.Exoneradas,")
            .append("Serv.Gravados,Serv.Exentos,Serv.Exonerados,")
            .append("Sub.Gravado,Sub.Exento,Sub.Exonerado,")
            .append("Venta Neta,Impuestos,Descuentos,Exonerado,Total\n");
        for (ReporteVentasLineaDTO l : datos.getLineas()) {
            csv.append(String.join(",", l.getClave(), l.getConsecutivo(), l.getTipoDocumento(),
                    l.getFechaEmision().toString(), l.getActividadEconomicaCodigo(),
                    l.getClienteNombre(), l.getClienteIdentificacion()))
                .append(",");
            csv.append(String.format("%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,",
                safe(l.getTotalMercanciasGravadas()), safe(l.getTotalMercanciasExentas()),
                safe(l.getTotalMercanciasExoneradas()), safe(l.getTotalServiciosGravados()),
                safe(l.getTotalServiciosExentos()), safe(l.getTotalServiciosExonerados())));
            csv.append(String.format("%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f\n",
                safe(l.getSubtotalGravado()), safe(l.getSubtotalExento()), safe(l.getSubtotalExonerado()),
                safe(l.getTotalVentaNeta()), safe(l.getTotalImpuesto()), safe(l.getTotalDescuentos()),
                safe(l.getMontoTotalExonerado()), safe(l.getTotalComprobante())));
        }
        datos.setArchivoGenerado(csv.toString().getBytes());
        datos.setNombreArchivo("ventas_" + datos.getFechaDesde() + "_" + datos.getFechaHasta() + ".csv");
        datos.setTipoContenido("text/csv");
    }

    private double safe(BigDecimal v) { return v != null ? v.doubleValue() : 0.0; }
}