package com.snnsoluciones.backnathbitpos.service.reportes.impl;

import com.snnsoluciones.backnathbitpos.dto.reporte.*;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.enums.mh.MedioPago;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.security.SecurityUtils;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import com.snnsoluciones.backnathbitpos.service.impl.SecurityContextService;
import com.snnsoluciones.backnathbitpos.service.pdf.PdfGeneratorService;
import com.snnsoluciones.backnathbitpos.service.reportes.ReporteVentasTipoPagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteVentasTipoPagoServiceImpl implements ReporteVentasTipoPagoService {

  private final SucursalRepository sucursalRepository;
  private final UsuarioRepository usuarioRepository;
  private final JdbcTemplate jdbcTemplate;
  private final PdfGeneratorService pdfGenerator;
  private final SecurityContextService securityContextService;
  private final StorageService storageService;

  @Override
  @Transactional(readOnly = true)
  public ReporteVentasTipoPagoResponse generarReporte(ReporteVentasTipoPagoRequest request)
      throws JRException {

    log.info("Generando reporte de ventas por tipo de pago para sucursal {}",
        request.getSucursalId());

    // Validar acceso
    validarAccesoSucursal(request.getSucursalId());

    // Obtener datos
    ReporteVentasTipoPagoResponse datos = obtenerDatosReporte(request);

    // Generar archivo según formato
    switch (request.getFormato()) {
      case PDF:
        datos.setArchivoGenerado(generarPdf(datos, request));
        datos.setNombreArchivo(
            "ventas_tipo_pago_" + request.getFechaDesde() + "_" + request.getFechaHasta() + ".pdf");
        datos.setTipoContenido("application/pdf");
        break;

      case EXCEL:
        datos.setArchivoGenerado(generarExcel(datos, request));
        datos.setNombreArchivo(
            "ventas_tipo_pago_" + request.getFechaDesde() + "_" + request.getFechaHasta()
                + ".xlsx");
        datos.setTipoContenido("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        break;

      case JSON:
        // Solo retorna los datos
        break;
    }

    return datos;
  }

  @Override
  @Transactional(readOnly = true)
  public ReporteVentasTipoPagoResponse obtenerDatosReporte(ReporteVentasTipoPagoRequest request) {

    // Obtener sucursal y empresa
    Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
        .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada"));

    // Query SQL para obtener ventas agrupadas por tipo de pago
    String sql = """
            SELECT 
                fmp.medio_pago as tipo_pago,
                COUNT(DISTINCT f.id) as cantidad_documentos,
                SUM(fmp.monto) as monto_total
            FROM factura_medios_pago fmp
            INNER JOIN facturas f ON fmp.factura_id = f.id
            WHERE f.sucursal_id = ?
              AND DATE(f.fecha_emision) >= ?
              AND DATE(f.fecha_emision) <= ?
              AND f.tipo_documento IN ('FE', 'TE')
              %s
            GROUP BY fmp.medio_pago
            ORDER BY monto_total DESC
        """.formatted(
        request.isIncluirAnuladas() ? "" : "AND f.estado NOT IN ('ANULADA', 'RECHAZADA')");

    List<Map<String, Object>> resultados = jdbcTemplate.queryForList(
        sql,
        request.getSucursalId(),
        request.getFechaDesde(),
        request.getFechaHasta()
    );

    // Calcular total general
    BigDecimal totalGeneral = resultados.stream()
        .map(r -> (BigDecimal) r.get("monto_total"))
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    Integer totalDocs = resultados.stream()
        .map(r -> ((Long) r.get("cantidad_documentos")).intValue())
        .reduce(0, Integer::sum);

    // Mapear resultados
    List<VentasPorTipoPagoDTO> detalles = resultados.stream()
        .map(row -> {
          String codigoMedioPago = (String) row.get("tipo_pago");
          BigDecimal monto = (BigDecimal) row.get("monto_total");
          Integer cantidad = ((Long) row.get("cantidad_documentos")).intValue();

          // Calcular porcentaje
          BigDecimal porcentaje = BigDecimal.ZERO;
          if (totalGeneral.compareTo(BigDecimal.ZERO) > 0) {
            porcentaje = monto.divide(totalGeneral, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100))
                .setScale(2, RoundingMode.HALF_UP);
          }

          return VentasPorTipoPagoDTO.builder()
              .medioPago(codigoMedioPago)
              .descripcion(obtenerDescripcionMedioPago(codigoMedioPago))
              .cantidadDocumentos(cantidad)
              .montoTotal(monto)
              .porcentaje(porcentaje)
              .build();
        })
        .collect(Collectors.toList());

    // Obtener usuario actual
    String usuarioActual;
    try {
      Usuario usuario = securityContextService.getCurrentUser();
      usuarioActual = usuario.getNombre() + " " + usuario.getApellidos();
    } catch (Exception e) {
      // Si falla, usa SecurityUtils
      usuarioActual = SecurityUtils.getCurrentUserLogin();
    }

    return ReporteVentasTipoPagoResponse.builder()
        .sucursalNombre(sucursal.getNombre())
        .empresaNombre(sucursal.getEmpresa().getNombreComercial())
        .empresaIdentificacion(sucursal.getEmpresa().getIdentificacion())
        .fechaGeneracion(LocalDateTime.now())
        .usuarioGenera(usuarioActual)
        .fechaDesde(request.getFechaDesde().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        .fechaHasta(request.getFechaHasta().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        .detalles(detalles)
        .totalGeneral(totalGeneral)
        .totalDocumentos(totalDocs)
        .build();
  }

  private byte[] generarPdf(ReporteVentasTipoPagoResponse datos,
      ReporteVentasTipoPagoRequest request)
      throws JRException {

    try {
      // Obtener sucursal para el logo
      Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
          .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada"));

      // Parámetros del reporte
      Map<String, Object> parametros = new HashMap<>();
      parametros.put("empresaNombre", datos.getEmpresaNombre());
      parametros.put("empresaIdentificacion", datos.getEmpresaIdentificacion());
      parametros.put("sucursalNombre", datos.getSucursalNombre());
      parametros.put("fechaDesde", request.getFechaDesde());
      parametros.put("fechaHasta", request.getFechaHasta());
      parametros.put("usuarioGenera", datos.getUsuarioGenera());
      parametros.put("totalGeneral", datos.getTotalGeneral());

      String logo =
          sucursal.getEmpresa().getLogoUrl() != null ? sucursal.getEmpresa().getLogoUrl() : "";
      byte[] logoByte = this.storageService.downloadFileAsBytes(logo);

      // Logo si existe
      parametros.put("logo", logoByte);

      // Generar PDF usando el servicio existente
      JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(datos.getDetalles());
      return pdfGenerator.generarReporte("ventas_por_tipo_pago", parametros, dataSource);

    } catch (Exception e) {
      log.error("Error generando PDF de ventas por tipo de pago", e);
      throw new JRException("Error al generar PDF: " + e.getMessage(), e);
    }
  }

  private byte[] generarExcel(ReporteVentasTipoPagoResponse datos,
      ReporteVentasTipoPagoRequest request)
      throws JRException {

    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      // Obtener reporte compilado
      JasperReport jasperReport = pdfGenerator.obtenerReporteCompilado("ventas_por_tipo_pago");

      // Parámetros
      Map<String, Object> parametros = new HashMap<>();
      parametros.put("empresaNombre", datos.getEmpresaNombre());
      parametros.put("empresaIdentificacion", datos.getEmpresaIdentificacion());
      parametros.put("sucursalNombre", datos.getSucursalNombre());
      parametros.put("fechaDesde", request.getFechaDesde());
      parametros.put("fechaHasta", request.getFechaHasta());
      parametros.put("usuarioGenera", datos.getUsuarioGenera());
      parametros.put("totalGeneral", datos.getTotalGeneral());

      // Llenar reporte
      JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(datos.getDetalles());
      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, dataSource);

      // Configurar exportador Excel
      JRXlsxExporter exporter = new JRXlsxExporter();
      exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
      exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));

      // Configuración para Excel
      SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
      configuration.setOnePagePerSheet(false);
      configuration.setRemoveEmptySpaceBetweenRows(true);
      configuration.setRemoveEmptySpaceBetweenColumns(true);
      configuration.setWhitePageBackground(false);
      configuration.setDetectCellType(true);
      configuration.setFontSizeFixEnabled(true);
      configuration.setIgnoreGraphics(false);
      exporter.setConfiguration(configuration);

      // Exportar
      exporter.exportReport();

      return outputStream.toByteArray();

    } catch (Exception e) {
      log.error("Error generando Excel de ventas por tipo de pago", e);
      throw new JRException("Error al generar Excel: " + e.getMessage(), e);
    }
  }

  private String obtenerDescripcionMedioPago(String codigo) {
    try {
      MedioPago mp = MedioPago.fromCodigo(codigo);
      return mp.getDescripcion();
    } catch (Exception e) {
      return "Desconocido (" + codigo + ")";
    }
  }

  @Override
  public void validarAccesoSucursal(Long sucursalId) {
    // Implementar validación según tu lógica de seguridad
    // Similar a lo que ya tienes en ReporteVentasServiceImpl
    log.debug("Validando acceso a sucursal {}", sucursalId);
    // Por ahora retorna true, pero debes implementar la lógica real
  }
}
