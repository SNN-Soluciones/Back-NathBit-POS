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

    // 1) Sucursal
    Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
        .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada"));

    // 2) Rango [desde 00:00, hasta 23:59:59]
    var desde = request.getFechaDesde().atStartOfDay();
    var hasta = request.getFechaHasta().atTime(23, 59, 59);  // 👈 CAMBIO: inclusivo

    // 3) SQL MEJORADO: Facturas electrónicas (solo ACEPTADAS) + Facturas internas
    String sql = """
        WITH 
        -- 👉 Facturas ELECTRÓNICAS (solo ACEPTADAS en Hacienda)
        facturas_electronicas AS (
          SELECT f.id
          FROM facturas f
          JOIN factura_bitacora fb 
            ON fb.factura_id = f.id 
           AND fb.estado = 'ACEPTADA'                    -- 🆕 Solo facturas aceptadas
          WHERE f.sucursal_id = ?
            AND f.fecha_emision >= ?
            AND f.fecha_emision <= ?                     -- 👈 CAMBIO: <= inclusivo
            AND f.tipo_documento IN ('FACTURA_ELECTRONICO','TIQUETE_ELECTRONICO')
        ),
        
        -- 🆕 Facturas INTERNAS (no anuladas)
        facturas_internas AS (
          SELECT fi.id
          FROM factura_interna fi
          WHERE fi.sucursal_id = ?
            AND fi.fecha >= ?
            AND fi.fecha <= ?                            -- 👈 CAMBIO: <= inclusivo
            AND fi.estado != 'ANULADA'
        ),
        
        -- 👉 Medios de pago de ELECTRÓNICAS
        medios_electronicas AS (
          SELECT 
            CASE
              WHEN p.medio_pago IN ('SINPE','SINPE_MOVIL') THEN 'SINPE_MOVIL'
              WHEN p.medio_pago IN ('DEPOSITO','TRANSFERENCIA_BANCARIA') THEN 'TRANSFERENCIA'
              ELSE p.medio_pago
            END AS medio_norm,
            p.factura_id AS documento_id,
            p.monto
          FROM factura_medios_pago p
          JOIN facturas_electronicas fe ON fe.id = p.factura_id
        ),
        
        -- 🆕 Medios de pago de INTERNAS
        medios_internos AS (
          SELECT 
            CASE
              WHEN mp.tipo IN ('SINPE','SINPE_MOVIL') THEN 'SINPE_MOVIL'
              WHEN mp.tipo IN ('DEPOSITO','TRANSFERENCIA_BANCARIA','TRANSFERENCIA') THEN 'TRANSFERENCIA'
              ELSE UPPER(mp.tipo)
            END AS medio_norm,
            mp.factura_interna_id AS documento_id,
            mp.monto
          FROM factura_interna_medios_pago mp
          JOIN facturas_internas fi ON fi.id = mp.factura_interna_id
        ),
        
        -- 🔗 UNIÓN de ambos tipos
        medios_todos AS (
          SELECT medio_norm, documento_id, monto FROM medios_electronicas
          UNION ALL
          SELECT medio_norm, documento_id, monto FROM medios_internos
        ),
        
        -- 📊 Agregación final
        agg AS (
          SELECT
            medio_norm,
            COUNT(DISTINCT documento_id) AS cantidad_documentos,
            SUM(monto) AS monto_total
          FROM medios_todos
          GROUP BY medio_norm
        )
        
        SELECT 
          m.medio_pago,
          COALESCE(agg.cantidad_documentos, 0) AS cantidad_documentos,
          COALESCE(agg.monto_total, 0) AS monto_total
        FROM (VALUES 
          ('EFECTIVO'),
          ('TARJETA'),
          ('TRANSFERENCIA'),
          ('SINPE_MOVIL')
        ) AS m(medio_pago)
        LEFT JOIN agg ON agg.medio_norm = m.medio_pago
        ORDER BY ARRAY_POSITION(
          ARRAY['EFECTIVO','TARJETA','TRANSFERENCIA','SINPE_MOVIL'], 
          m.medio_pago
        )
        """;

    // 4) Ejecutar query con 6 parámetros
    List<VentasPorTipoPagoDTO> detalles = jdbcTemplate.query(
        sql,
        ps -> {
          // Parámetros para facturas ELECTRÓNICAS
          ps.setLong(1, request.getSucursalId());
          ps.setTimestamp(2, Timestamp.valueOf(desde));
          ps.setTimestamp(3, Timestamp.valueOf(hasta));      // 👈 CAMBIO: usa 'hasta'

          // Parámetros para facturas INTERNAS
          ps.setLong(4, request.getSucursalId());
          ps.setTimestamp(5, Timestamp.valueOf(desde));
          ps.setTimestamp(6, Timestamp.valueOf(hasta));      // 👈 CAMBIO: usa 'hasta'
        },
        (rs, i) -> {
          String medioEnum = rs.getString("medio_pago");
          int cantidad = rs.getInt("cantidad_documentos");
          BigDecimal monto = rs.getBigDecimal("monto_total");

          return VentasPorTipoPagoDTO.builder()
              .medioPago(medioEnum)
              .descripcion(obtenerDescripcionMedioPago(medioEnum))
              .cantidadDocumentos(cantidad)
              .montoTotal(monto != null ? monto.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
              .build();
        }
    );

    // 5) Totales y porcentajes
    BigDecimal totalGeneral = detalles.stream()
        .map(VentasPorTipoPagoDTO::getMontoTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(2, RoundingMode.HALF_UP);

    detalles.forEach(d -> {
      if (totalGeneral.compareTo(BigDecimal.ZERO) > 0) {
        d.setPorcentaje(
            d.getMontoTotal()
                .divide(totalGeneral, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP)
        );
      } else {
        d.setPorcentaje(BigDecimal.ZERO);
      }
    });

    // 6) 🆕 NUEVO: Contar documentos de AMBOS tipos con mismo criterio
    String countSql = """
      SELECT 
        (
          -- Contar facturas ELECTRÓNICAS (solo ACEPTADAS)
          SELECT COUNT(DISTINCT f.id)
          FROM facturas f
          JOIN factura_bitacora fb 
            ON fb.factura_id = f.id 
           AND fb.estado = 'ACEPTADA'                    -- 🆕 Solo aceptadas
          WHERE f.sucursal_id = ?
            AND f.fecha_emision >= ?
            AND f.fecha_emision <= ?                     -- 👈 CAMBIO: <= inclusivo
            AND f.tipo_documento IN ('FACTURA_ELECTRONICO','TIQUETE_ELECTRONICO')
        )
        +
        (
          -- Contar facturas INTERNAS (no anuladas)
          SELECT COUNT(DISTINCT fi.id)
          FROM factura_interna fi
          WHERE fi.sucursal_id = ?
            AND fi.fecha >= ?
            AND fi.fecha <= ?                            -- 👈 CAMBIO: <= inclusivo
            AND fi.estado != 'ANULADA'
        ) AS total_documentos
      """;

    Integer totalDocs = jdbcTemplate.queryForObject(
        countSql,
        new Object[]{
            // Parámetros para electrónicas
            request.getSucursalId(),
            Timestamp.valueOf(desde),
            Timestamp.valueOf(hasta),                      // 👈 CAMBIO: usa 'hasta'
            // Parámetros para internas
            request.getSucursalId(),
            Timestamp.valueOf(desde),
            Timestamp.valueOf(hasta)                       // 👈 CAMBIO: usa 'hasta'
        },
        Integer.class
    );

    // 7) Usuario que genera
    String usuarioActual;
    try {
      Usuario usuario = securityContextService.getCurrentUser();
      usuarioActual = usuario.getNombre() + " " + usuario.getApellidos();
    } catch (Exception e) {
      usuarioActual = SecurityUtils.getCurrentUserLogin();
    }

    // 8) Armar response
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    return ReporteVentasTipoPagoResponse.builder()
        .sucursalNombre(sucursal.getNombre())
        .empresaNombre(sucursal.getEmpresa().getNombreComercial())
        .empresaIdentificacion(sucursal.getEmpresa().getIdentificacion())
        .fechaGeneracion(LocalDateTime.now())
        .usuarioGenera(usuarioActual)
        .fechaDesde(request.getFechaDesde().format(fmt))
        .fechaHasta(request.getFechaHasta().format(fmt))
        .detalles(detalles)
        .totalGeneral(totalGeneral)
        .totalDocumentos(totalDocs != null ? totalDocs : 0)
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

      String logoPath = sucursal.getEmpresa().getLogoUrl();
      byte[] logoBytes = null;
      if (logoPath != null && !logoPath.isBlank()) {
        try { logoBytes = storageService.downloadFileAsBytes(logoPath); } catch (Exception ignore) {}
      }
      parametros.put("logo", (logoBytes != null && logoBytes.length > 0) ? new ByteArrayInputStream(logoBytes) : null);

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

  private String obtenerDescripcionMedioPago(String enumValue) {
    try {
      // El valor viene como EFECTIVO, TARJETA, etc. (nombre del enum)
      MedioPago mp = MedioPago.valueOf(enumValue);
      return mp.getDescripcion();
    } catch (Exception e) {
      // Si falla, intentar devolver algo legible
      if (enumValue != null) {
        // Convertir EFECTIVO -> Efectivo, TARJETA -> Tarjeta, etc.
        return enumValue.substring(0, 1).toUpperCase() +
            enumValue.substring(1).toLowerCase().replace("_", " ");
      }
      return "Desconocido";
    }
  }

  // También necesitas agregar este método para obtener el código si lo necesitas
  private String obtenerCodigoMedioPago(String enumValue) {
    try {
      MedioPago mp = MedioPago.valueOf(enumValue);
      return mp.getCodigo();
    } catch (Exception e) {
      return "99"; // Código para "Otros"
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
