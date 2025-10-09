package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.facturarecepcion.*;
import com.snnsoluciones.backnathbitpos.dto.facturarecepcion.DecisionMensajeRequest.TipoDecision;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.factura.EstadoFacturaRecepcion;
import com.snnsoluciones.backnathbitpos.enums.factura.TipoMensajeReceptor;
import com.snnsoluciones.backnathbitpos.enums.mh.EstadoCompra;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoCompra;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.HaciendaClient;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.IdentificacionDTO;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.scheduler.FacturaRecepcionXMLParserService;
import com.snnsoluciones.backnathbitpos.sign.SignerService;
import java.io.ByteArrayInputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FacturaRecepcionService {

  private final FacturaRecepcionRepository facturaRecepcionRepository;
  private final EmpresaRepository empresaRepository;
  private final SucursalRepository sucursalRepository;
  private final ProveedorRepository proveedorRepository;
  private final CompraRepository compraRepository;
  private final MetricaCompraMensualRepository metricasRepository;
  private final UsuarioRepository usuarioRepository;
  private final CompraDetalleRepository compraDetalleRepository;
  private final ProductoInventarioService productoInventarioService;
  private final TerminalService terminalService;

  private final FacturaRecepcionXMLParserService xmlParserService;
  private final StorageService storageService;
  private final HaciendaClient haciendaClient;
  private final SignerService signerService;
  private final FacturaRecepcionExcelService facturaRecepcionExcelService;

  public Optional<FacturaRecepcion> buscarPorId(Long id) {
    return facturaRecepcionRepository.findById(id);
  }

  /**
   * Listar facturas recibidas con filtros
   */
  public Page<FacturaRecepcionListResponse> listar(
      Long empresaId,
      Long sucursalId,
      EstadoFacturaRecepcion estado,
      LocalDate fechaInicio,
      LocalDate fechaFin,
      Pageable pageable  // ✅ Recibir Pageable directamente del Controller
  ) {
    log.info("Listando facturas - empresa: {}, sucursal: {}, estado: {}",
        empresaId, sucursalId, estado);

    // Llamar repository con Pageable
    Page<FacturaRecepcion> pageResult = facturaRecepcionRepository.findByFiltros(
        empresaId,
        sucursalId,
        estado,
        fechaInicio,
        fechaFin,
        pageable
    );

    // Mapear a DTO
    return pageResult.map(this::toListResponse);
  }


  /**
   * Obtener detalle completo de factura
   */
  @Transactional(readOnly = true)
  public FacturaRecepcionResponse obtenerDetalle(Long id) {
    FacturaRecepcion factura = facturaRecepcionRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Factura no encontrada"));

    return mapearAResponse(factura);
  }

  /**
   * Tomar decisión y enviar mensaje receptor a Hacienda
   */
  @Transactional
  public MensajeReceptorResponse tomarDecision(Long id, DecisionMensajeRequest request) {
    log.info("Procesando decisión para factura recepción ID: {}, decisión: {}",
        id, request.getDecision());

    FacturaRecepcion factura = facturaRecepcionRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Factura no encontrada"));

    // Validar estado
    if (factura.getEstadoInterno() != EstadoFacturaRecepcion.PENDIENTE_DECISION) {
      throw new RuntimeException("La factura ya fue procesada");
    }

    // 👇 AQUÍ VAN LAS VALIDACIONES
    // ==================== VALIDACIONES ====================

    // Validar razón para rechazo y parcial
    if (request.getDecision() == TipoDecision.RECHAZAR ||
        request.getDecision() == TipoDecision.PARCIAL) {

      // Validar razón obligatoria
      if (request.getRazon() == null || request.getRazon().isBlank()) {
        throw new RuntimeException("La razón es obligatoria para rechazo y aceptación parcial");
      }

      // Validar longitud según Hacienda (5-160 caracteres)
      if (request.getRazon().length() < 5 || request.getRazon().length() > 160) {
        throw new RuntimeException("La razón debe tener entre 5 y 160 caracteres");
      }
    }

    // Validar montos para aceptación parcial
    if (request.getDecision() == TipoDecision.PARCIAL) {
      // Validar montos obligatorios
      if (request.getMontoAceptado() == null) {
        throw new RuntimeException("El monto aceptado es obligatorio para aceptación parcial");
      }

      if (request.getMontoIvaAceptado() == null) {
        throw new RuntimeException(
            "El monto de IVA aceptado es obligatorio para aceptación parcial");
      }

      // Validar que no excedan los montos originales
      if (request.getMontoAceptado().compareTo(factura.getTotalComprobante()) > 0) {
        throw new RuntimeException("El monto aceptado no puede ser mayor al total de la factura");
      }

      if (request.getMontoIvaAceptado().compareTo(factura.getTotalImpuesto()) > 0) {
        throw new RuntimeException("El IVA aceptado no puede ser mayor al IVA total de la factura");
      }
    }

    // ==================== PROCESAR SEGÚN DECISIÓN ====================

// Determinar tipo de mensaje
    TipoMensajeReceptor tipoMensaje;

    switch (request.getDecision()) {
      case ACEPTAR:
        factura.setEstadoInterno(EstadoFacturaRecepcion.ACEPTADA);
        factura.setMotivoRespuesta(request.getRazon());
        tipoMensaje = TipoMensajeReceptor.ACEPTADO;
        break;

      case PARCIAL:
        factura.setEstadoInterno(EstadoFacturaRecepcion.ACEPTADA_PARCIAL);
        factura.setMotivoRespuesta(request.getRazon());
        factura.setMontoTotalAceptado(request.getMontoAceptado());
        factura.setMontoImpuestoAceptado(request.getMontoIvaAceptado());
        tipoMensaje = TipoMensajeReceptor.ACEPTADO_PARCIAL;
        break;

      case RECHAZAR:
        factura.setEstadoInterno(EstadoFacturaRecepcion.RECHAZADA);
        factura.setMotivoRespuesta(request.getRazon());
        tipoMensaje = TipoMensajeReceptor.RECHAZADO;
        break;

      default:
        throw new RuntimeException("Decisión no válida");
    }

    // ==================== CONSTRUIR Y ENVIAR MENSAJE ====================

    try {
      // 1. Construir XML mensaje receptor
      String xmlSinFirmar = construirXmlMensajeReceptor(factura, tipoMensaje, request);

      // 2. Firmar XML
      byte[] xmlFirmado = signerService.signXmlForEmpresa(
          xmlSinFirmar.getBytes(StandardCharsets.UTF_8),
          factura.getEmpresa().getId(),
          TipoDocumento.MENSAJE_RECEPTOR
      );

      String xmlMensajeFirmado = new String(xmlFirmado, StandardCharsets.UTF_8);

      // 3. Enviar a Hacienda
      IdentificacionDTO receptor = IdentificacionDTO.builder()
          .tipoIdentificacion(
              factura.getProveedorTipoIdentificacion().getCodigo()) // asegurar que sea 01/02/03/...
          .numeroIdentificacion(factura.getProveedorIdentificacion())
          .build();

      String respuestaHacienda = haciendaClient.enviarMensajeReceptor(
          factura.getEmpresa().getId(),
          xmlMensajeFirmado,
          receptor
      );

      // 4. ✅ Subir XML firmado (usa sobrecarga byte[])
      String pathXmlFirmado = subirArchivoS3(
          factura.getEmpresa().getNombreRazonSocial(),
          "MENSAJE_RECEPTOR",
          factura.getClave() + "-mensaje",
          "xml",
          xmlFirmado  // ✅ byte[] directo
      );

      // 5. ✅ Subir respuesta de Hacienda (usa sobrecarga String)
      String pathXmlRespuestaMh = subirArchivoS3(
          factura.getEmpresa().getNombreRazonSocial(),
          "RESPUESTA_MH",
          factura.getClave() + "-respuesta",
          "xml",
          respuestaHacienda  // ✅ String directo
      );

      // 6. Actualizar factura con las rutas
      factura.setMensajeReceptorEnviado(true);
      factura.setTipoMensajeReceptor(tipoMensaje);
      factura.setXmlMensajeReceptorPath(pathXmlFirmado);  // 👈 AGREGAR CAMPO
      factura.setXmlRespuestaHaciendaPath(pathXmlRespuestaMh);  // 👈 AGREGAR CAMPO

      facturaRecepcionRepository.save(factura);

      log.info("✅ Mensaje receptor enviado. XML firmado: {}, Respuesta MH: {}",
          pathXmlFirmado, pathXmlRespuestaMh);

      return MensajeReceptorResponse.builder()
          .exitoso(true)
          .mensaje("Decisión procesada exitosamente")
          .respuestaHacienda(tipoMensaje.name())
          .build();

    } catch (Exception e) {
      log.error("Error enviando mensaje receptor", e);
      throw new RuntimeException("Error al procesar decisión: " + e.getMessage());
    }
  }

  /**
   * Convertir factura aceptada a Compra
   */
  @Transactional
  public ConvertirCompraResponse convertirACompra(Long facturaRecepcionId) {
    log.info("Convirtiendo factura recepción {} a compra", facturaRecepcionId);

    FacturaRecepcion factura = facturaRecepcionRepository.findById(facturaRecepcionId)
        .orElseThrow(() -> new RuntimeException("Factura no encontrada"));

    // Validaciones
    if (factura.getEstadoInterno() != EstadoFacturaRecepcion.ACEPTADA) {
      throw new RuntimeException("Solo se pueden convertir facturas ACEPTADAS");
    }

    if (factura.getConvertidaCompra()) {
      throw new RuntimeException("Esta factura ya fue convertida a compra");
    }

    if (factura.getProveedor() == null) {
      throw new RuntimeException("Debe asignar un proveedor antes de convertir a compra");
    }

    try {
      // Crear Compra
      Compra compra = new Compra();
      compra.setEmpresa(factura.getEmpresa());
      compra.setSucursal(factura.getSucursal());
      compra.setProveedor(factura.getProveedor());
      compra.setUsuario(factura.getCompra().getUsuario());

      compra.setTipoDocumentoHacienda(factura.getTipoDocumento());
      compra.setClaveHacienda(factura.getClave());
      compra.setNumeroDocumento(factura.getNumeroConsecutivo());
      compra.setFechaEmision(factura.getFechaEmision());
      compra.setFechaRecepcion(LocalDateTime.now());

      compra.setCondicionVenta(factura.getCondicionVenta());
      compra.setPlazoCredito(factura.getPlazoCredito());

      if (factura.getMoneda() != null) {
        compra.setMoneda(factura.getMoneda());
      }
      compra.setTipoCambio(
          factura.getTipoCambio() != null ? factura.getTipoCambio() : BigDecimal.ONE);

      // Totales
      compra.setTotalGravado(factura.getTotalGravado());
      compra.setTotalExento(factura.getTotalExento());
      compra.setTotalExonerado(factura.getTotalExonerado());
      compra.setTotalVenta(factura.getTotalVenta());
      compra.setTotalDescuentos(factura.getTotalDescuentos());
      compra.setTotalVentaNeta(factura.getTotalVentaNeta());
      compra.setTotalImpuesto(factura.getTotalImpuesto());
      compra.setTotalOtrosCargos(factura.getTotalOtrosCargos());
      compra.setTotalComprobante(factura.getTotalComprobante());

      compra = compraRepository.save(compra);

      // Vincular
      factura.setCompra(compra);
      factura.setConvertidaCompra(true);
      facturaRecepcionRepository.save(factura);

      try {
        productoInventarioService.procesarCompra(compra);

        // Marcar como completada solo si inventario se procesó OK
        compra.setEstado(EstadoCompra.COMPLETADA);
        compraRepository.save(compra);

        log.info("✅ Inventario actualizado automáticamente para compra {}", compra.getId());

      } catch (Exception e) {
        log.error("⚠️ Error actualizando inventario para compra {}: {}",
            compra.getId(), e.getMessage());
        // La compra queda en ACEPTADA pero sin inventario procesado
        // El usuario puede procesarlo manualmente después
      }

      // Actualizar métricas mensuales por fecha de FACTURA
      actualizarMetricasMensuales(compra);

      log.info("Factura convertida a compra exitosamente. Compra ID: {}", compra.getId());

      return ConvertirCompraResponse.builder()
          .compraId(compra.getId())
          .facturaRecepcionId(factura.getId())
          .mensaje("Factura convertida exitosamente a compra")
          .build();

    } catch (Exception e) {
      log.error("Error convirtiendo a compra", e);
      throw new RuntimeException("Error al convertir a compra: " + e.getMessage());
    }
  }

  // ==================== MÉTODOS PRIVADOS ====================

  private String subirArchivoS3(String razonSocial, String tipoDoc, String clave,
      String extension, MultipartFile file) throws IOException {
    String carpeta = String.format("%s/compras/%s/%s/",
        razonSocial,
        tipoDoc,
        LocalDate.now().getYear()
    );
    String key = carpeta + clave + "." + extension;

    // Usar el método con InputStream
    storageService.uploadFile(
        file.getInputStream(),
        key,
        file.getContentType(),
        file.getSize()
    );

    return key;
  }

  private String subirArchivoS3(String razonSocial, String tipoDoc, String clave,
      String extension, byte[] contenido) throws IOException {
    String carpeta = String.format("%s/compras/%s/%s/",
        razonSocial,
        tipoDoc,
        LocalDate.now().getYear()
    );
    String key = carpeta + clave + "." + extension;

    storageService.uploadFile(
        new ByteArrayInputStream(contenido),
        key,
        "application/xml", // Content type para XML
        (long) contenido.length
    );

    return key;
  }

  private String subirArchivoS3(String razonSocial, String tipoDoc, String clave,
      String extension, String contenido) throws IOException {
    return subirArchivoS3(
        razonSocial,
        tipoDoc,
        clave,
        extension,
        contenido.getBytes(StandardCharsets.UTF_8)
    );
  }

  private Proveedor buscarOCrearProveedor(FacturaRecepcion factura, Empresa empresa,
      boolean crearSiNoExiste) {
    Proveedor proveedor = proveedorRepository
        .findByNumeroIdentificacionAndEmpresaId(factura.getProveedorIdentificacion(),
            empresa.getId())
        .orElse(null);

    if (proveedor == null && crearSiNoExiste) {
      proveedor = new Proveedor();
      proveedor.setEmpresa(empresa);
      proveedor.setTipoIdentificacion(factura.getProveedorTipoIdentificacion());
      proveedor.setNumeroIdentificacion(factura.getProveedorIdentificacion());
      proveedor.setRazonSocial(factura.getProveedorNombre());
      String nombreComercial = factura.getProveedorNombreComercial();
      if (nombreComercial == null || nombreComercial.trim().isEmpty()) {
        nombreComercial = factura.getProveedorNombre();
      }// 👈 PUEDE SER NULL
      proveedor.setNombreComercial(nombreComercial);
      proveedor.setEmail(factura.getProveedorEmail());
      proveedor.setTelefono(factura.getProveedorTelefono());
      proveedor.setActivo(true);

      proveedor = proveedorRepository.save(proveedor);  // 👈 AQUÍ FALLA
      log.info("Proveedor creado automáticamente: {}", proveedor.getNumeroIdentificacion());
    }

    return proveedor;
  }

  private String construirXmlMensajeReceptor(FacturaRecepcion factura,
      TipoMensajeReceptor tipo, DecisionMensajeRequest request) {

    // Fecha con TIMEZONE de Costa Rica
    ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Costa_Rica"));
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    String fechaFormateada = now.format(formatter);

    String codigoMensaje = tipo.getCodigo(); // "1", "2", o "3"

    // ==================== DETALLE MENSAJE ====================
    // SIEMPRE incluir DetalleMensaje (aunque sea opcional para código 1)
    String detalleMensaje;
    if (tipo == TipoMensajeReceptor.RECHAZADO || tipo == TipoMensajeReceptor.ACEPTADO_PARCIAL) {
      // Obligatorio para 2 y 3
      String razon = request != null && request.getRazon() != null
          ? request.getRazon().trim()
          : null;

      if (razon == null || razon.length() < 5) {
        detalleMensaje = tipo == TipoMensajeReceptor.RECHAZADO
            ? "Factura rechazada por discrepancias"
            : "Aceptacion parcial por ajustes";
      } else if (razon.length() > 160) {
        detalleMensaje = razon.substring(0, 160);
      } else {
        detalleMensaje = razon;
      }
    } else {
      // Para ACEPTADO (código 1) - opcional pero lo incluimos
      detalleMensaje = "Factura aceptada";
    }

    // ==================== MONTOS ====================
    // Estos campos SON OPCIONALES pero los incluimos siempre que haya datos
    BigDecimal montoTotalImpuesto = factura.getTotalImpuesto();
    BigDecimal totalFactura = factura.getTotalComprobante();

    // Para aceptación parcial, usar montos del request si existen
    if (tipo == TipoMensajeReceptor.ACEPTADO_PARCIAL && request != null) {
      if (request.getMontoIvaAceptado() != null) {
        montoTotalImpuesto = request.getMontoIvaAceptado();
      }
      if (request.getMontoAceptado() != null) {
        totalFactura = request.getMontoAceptado();
      }
    }

    // ==================== CÓDIGO ACTIVIDAD ====================
    // Actividad económica del receptor (tu empresa) para aplicar crédito fiscal
    String codigoActividad = factura.getCodigoActividad(); // De la factura

    // ==================== CONDICIÓN IMPUESTO ====================
    // 01 = Crédito fiscal (por defecto)
    // 02 = Gasto
    // 03 = Proporcionalidad
    // 04 = Crédito parcial y gasto
    // 05 = No genera crédito ni gasto (exento/exonerado)
    String condicionImpuesto = "01"; // Por defecto: Crédito fiscal

    // ==================== MONTO ACREDITABLE ====================
    BigDecimal montoImpuestoAcreditar = montoTotalImpuesto; // Por defecto todo el IVA
    BigDecimal montoGastoAplicable = null; // Opcional

    // ==================== CONSTRUCCIÓN XML ====================
    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xml.append("<MensajeReceptor ")
        .append(
            "xmlns=\"https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/mensajeReceptor\" ")
        .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
        .append(
            "xsi:schemaLocation=\"https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/mensajeReceptor MensajeReceptor_4.4.xsd\">\n");

    // ==================== ORDEN SEGÚN EJEMPLO REAL ====================

    // 1. Clave
    xml.append("  <Clave>").append(factura.getClave()).append("</Clave>\n");

    // 2. NumeroCedulaEmisor (proveedor - emisor del comprobante original)
    xml.append("  <NumeroCedulaEmisor>")
        .append(formatearCedulaConPadding(factura.getProveedorIdentificacion()))
        .append("</NumeroCedulaEmisor>\n");

    // 3. FechaEmisionDoc
    xml.append("  <FechaEmisionDoc>").append(fechaFormateada).append("</FechaEmisionDoc>\n");

    // 4. Mensaje
    xml.append("  <Mensaje>").append(codigoMensaje).append("</Mensaje>\n");

    // 5. DetalleMensaje (SIEMPRE incluir)
    xml.append("  <DetalleMensaje>").append(escapeXml(detalleMensaje))
        .append("</DetalleMensaje>\n");

    // 6. MontoTotalImpuesto (si existe)
    if (montoTotalImpuesto != null && montoTotalImpuesto.compareTo(BigDecimal.ZERO) > 0) {
      xml.append("  <MontoTotalImpuesto>")
          .append(formatearMonto(montoTotalImpuesto))
          .append("</MontoTotalImpuesto>\n");
    }

    // 7. CodigoActividad (si existe)
    if (codigoActividad != null && !codigoActividad.isEmpty()) {
      xml.append("  <CodigoActividad>").append(codigoActividad).append("</CodigoActividad>\n");
    }

    // 8. CondicionImpuesto
    xml.append("  <CondicionImpuesto>").append(condicionImpuesto).append("</CondicionImpuesto>\n");

    // 9. MontoTotalImpuestoAcreditar (si aplica crédito fiscal)
    if (montoImpuestoAcreditar != null && montoImpuestoAcreditar.compareTo(BigDecimal.ZERO) > 0) {
      xml.append("  <MontoTotalImpuestoAcreditar>")
          .append(formatearMonto(montoImpuestoAcreditar))
          .append("</MontoTotalImpuestoAcreditar>\n");
    }

    // 10. MontoTotalDeGastoAplicable (opcional - si parte se declara como gasto)
    if (montoGastoAplicable != null && montoGastoAplicable.compareTo(BigDecimal.ZERO) > 0) {
      xml.append("  <MontoTotalDeGastoAplicable>")
          .append(formatearMonto(montoGastoAplicable))
          .append("</MontoTotalDeGastoAplicable>\n");
    }

    // 11. TotalFactura (OBLIGATORIO)
    xml.append("  <TotalFactura>")
        .append(formatearMonto(totalFactura))
        .append("</TotalFactura>\n");

    // 12. NumeroCedulaReceptor (tu empresa - receptor del comprobante original)
    xml.append("  <NumeroCedulaReceptor>")
        .append(formatearCedulaConPadding(factura.getReceptorIdentificacion()))
        .append("</NumeroCedulaReceptor>\n");

    String consecutivo = terminalService.generarNumeroConsecutivo(1L,
        TipoDocumento.MENSAJE_RECEPTOR);
    // 13. NumConsecutivoReceptor (20 dígitos)
    xml.append("  <NumConsecutivoReceptor>").append(consecutivo)
        .append("</NumConsecutivoReceptor>\n");

    xml.append("</MensajeReceptor>");
    return xml.toString();
  }


  /**
   * Formatear cédula con padding a 12 caracteres Ejemplos: - 3101752961 (10 dígitos) → 003101752961
   * (12 dígitos) - 123456789 (9 dígitos)   → 000123456789 (12 dígitos) - 123456789012 (12)       →
   * 123456789012 (sin cambio)
   */
  private String formatearCedulaConPadding(String cedula) {
    if (cedula == null || cedula.isEmpty()) {
      throw new RuntimeException("Cédula no puede ser nula o vacía");
    }

    // Remover cualquier caracter no numérico
    String cedulaLimpia = cedula.replaceAll("[^0-9]", "");

    // Si ya tiene 12 o más dígitos, devolver como está (máximo 12)
    if (cedulaLimpia.length() >= 12) {
      return cedulaLimpia.substring(0, 12);
    }

    // Agregar padding de ceros a la izquierda hasta 12 caracteres
    return String.format("%012d", Long.parseLong(cedulaLimpia));
  }


  /**
   * Formatear monto a formato decimal 18,5 Ejemplo: 113.10000
   */
  private String formatearMonto(BigDecimal monto) {
    if (monto == null) {
      return "0.00000";
    }
    return String.format("%.5f", monto);
  }


  /**
   * Escapar caracteres especiales XML
   */
  private String escapeXml(String text) {
    if (text == null) {
      return "";
    }
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }

  private void actualizarMetricasMensuales(Compra compra) {
    int anio = compra.getFechaEmision().getYear();
    int mes = compra.getFechaEmision().getMonthValue();

    MetricaCompraMensual metricas = metricasRepository
        .findByEmpresaIdAndSucursalIdAndAnioAndMes(
            compra.getEmpresa().getId(),
            compra.getSucursal().getId(),
            anio,
            mes
        )
        .orElseGet(() -> {
          MetricaCompraMensual nueva = MetricaCompraMensual.builder()
              .empresa(compra.getEmpresa())
              .sucursal(compra.getSucursal())
              .anio(anio)
              .mes(mes)
              .totalCompras(0L)
              .montoTotal(BigDecimal.ZERO)
              .montoTotalImpuestos(BigDecimal.ZERO)
              .cantidadProveedores(0)
              .build();
          return nueva;
        });

    // Actualizar totales
    metricas.setMontoTotal(metricas.getMontoTotal().add(compra.getTotalComprobante()));
    metricas.setTotalCompras(metricas.getTotalCompras() + 1);
    metricas.setMontoTotalImpuestos(
        metricas.getMontoTotalImpuestos().add(compra.getTotalImpuesto())
    );

    // Recalcular cantidad de proveedores únicos
    long proveedoresUnicos = compraRepository.countDistinctProveedoresByMesAnio(
        compra.getEmpresa().getId(),
        compra.getSucursal().getId(),
        anio,
        mes
    );
    metricas.setCantidadProveedores((int) proveedoresUnicos);

    // Calcular top proveedor
    List<Object[]> topProveedor = compraRepository.findTopProveedorByMesAnio(
        compra.getEmpresa().getId(),
        compra.getSucursal().getId(),
        anio,
        mes
    );
    if (!topProveedor.isEmpty()) {
      Object[] top = topProveedor.get(0);
      metricas.setTopProveedorId((Long) top[0]);
      metricas.setTopProveedorMonto((BigDecimal) top[1]);
    }

    // Calcular top producto CABYS
    List<Object[]> topCabys = compraDetalleRepository.findTopCabysByMesAnio(
        compra.getEmpresa().getId(),
        compra.getSucursal().getId(),
        anio,
        mes
    );
    if (!topCabys.isEmpty()) {
      Object[] top = topCabys.get(0);
      metricas.setTopProductoCabys((String) top[0]);
      metricas.setTopProductoMonto((BigDecimal) top[1]);
    }

    metricasRepository.save(metricas);
    log.info("✅ Métricas mensuales actualizadas: {}/{} - {} compras, {} proveedores",
        mes, anio, metricas.getTotalCompras(), metricas.getCantidadProveedores());
  }

  private FacturaRecepcionResponse mapearAResponse(FacturaRecepcion factura) {
    return FacturaRecepcionResponse.builder()
        .id(factura.getId())
        .clave(factura.getClave())
        .tipoDocumento(factura.getTipoDocumento())
        .numeroConsecutivo(factura.getNumeroConsecutivo())
        .fechaEmision(factura.getFechaEmision())
        .estadoInterno(factura.getEstadoInterno())

        .proveedorId(factura.getProveedor() != null ? factura.getProveedor().getId() : null)
        .proveedorNombre(factura.getProveedorNombre())
        .proveedorIdentificacion(factura.getProveedorIdentificacion())
        .proveedorCorreo(factura.getProveedorEmail())

        .receptorNombre(factura.getReceptorNombre())
        .receptorIdentificacion(factura.getReceptorIdentificacion())

        .condicionVenta(factura.getCondicionVenta())
        .plazoCredito(factura.getPlazoCredito())

        .totalGravado(factura.getTotalGravado())
        .totalExento(factura.getTotalExento())
        .totalExonerado(factura.getTotalExonerado())
        .totalVenta(factura.getTotalVenta())
        .totalDescuentos(factura.getTotalDescuentos())
        .totalVentaNeta(factura.getTotalVentaNeta())
        .totalImpuesto(factura.getTotalImpuesto())
        .totalOtrosCargos(factura.getTotalOtrosCargos())
        .totalComprobante(factura.getTotalComprobante())

        .mensajeReceptorEnviado(factura.getMensajeReceptorEnviado())
        .tipoMensajeReceptor(
            factura.getTipoMensajeReceptor() != null ? factura.getTipoMensajeReceptor().name()
                : null)

        .convertidaACompra(factura.getConvertidaCompra())
        .compraId(factura.getCompra() != null ? factura.getCompra().getId() : null)

        .rutaXmlS3(factura.getXmlOriginalPath())
        .rutaPdfS3(factura.getPdfPath())

        .detalles(factura.getDetalles().stream()
            .map(this::mapearDetalleAResponse)
            .collect(Collectors.toList()))

        .build();
  }

  private FacturaRecepcionListResponse mapearAListResponse(FacturaRecepcion factura) {
    return FacturaRecepcionListResponse.builder()
        .id(factura.getId())
        .clave(factura.getClave())
        .numeroConsecutivo(factura.getNumeroConsecutivo())
        .fechaEmision(factura.getFechaEmision())
        .proveedorNombre(factura.getProveedorNombre())
        .proveedorIdentificacion(factura.getProveedorIdentificacion())
        .totalComprobante(factura.getTotalComprobante())
        .estadoInterno(factura.getEstadoInterno())
        .mensajeReceptorEnviado(factura.getMensajeReceptorEnviado())
        .convertidaACompra(factura.getConvertidaCompra())
        .build();
  }

  private FacturaRecepcionDetalleResponse mapearDetalleAResponse(FacturaRecepcionDetalle detalle) {
    return FacturaRecepcionDetalleResponse.builder()
        .id(detalle.getId())
        .numeroLinea(detalle.getNumeroLinea())
        .codigoCabys(detalle.getCodigoCabys())
        .codigoComercial(detalle.getCodigoComercial())
        .tipoCodigoComercial(detalle.getTipoCodigoComercial())
        .detalle(detalle.getDetalle())
        .cantidad(detalle.getCantidad())
        .unidadMedida(detalle.getUnidadMedida().name())
        .unidadMedidaComercial(detalle.getUnidadMedidaComercial())
        .precioUnitario(detalle.getPrecioUnitario())
        .montoTotal(detalle.getMontoTotal())
        .subTotal(detalle.getSubtotal())
        .montoDescuento(detalle.getMontoDescuento())
        .montoTotalLinea(detalle.getMontoTotalLinea())
        .productoNombre(detalle.getDescripcion() != null ? detalle.getDescripcion() : null)
        .build();
  }

  /**
   * SOLO para MailReceptor - Guarda sin procesar
   */
  @Transactional
  public FacturaRecepcionResponse guardarDesdeEmail(SubirXmlRequest request) {
    log.info("Guardando factura desde email para empresa: {}, sucursal: {}",
        request.getEmpresaId(), request.getSucursalId());

    try {
      // 1. Validar empresa y sucursal
      Empresa empresa = empresaRepository.findById(request.getEmpresaId())
          .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

      Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
          .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));

      // 2. Parsear XML para extraer la clave
      // 2. Parsear XML - LIMPIAR BOM
      byte[] xmlBytes = request.getXmlFile().getBytes();

      // Remover BOM si existe
      if (xmlBytes.length >= 3 &&
          xmlBytes[0] == (byte) 0xEF &&
          xmlBytes[1] == (byte) 0xBB &&
          xmlBytes[2] == (byte) 0xBF) {
        // Tiene BOM UTF-8, saltarlo
        xmlBytes = Arrays.copyOfRange(xmlBytes, 3, xmlBytes.length);
      }

      String xmlContent = new String(xmlBytes, StandardCharsets.UTF_8).trim();
      FacturaRecepcion factura = xmlParserService.parsearXML(xmlContent, empresa, sucursal);

      // 3. Si existe, retornar la existente
      Optional<FacturaRecepcion> facturaExistente =
          facturaRecepcionRepository.findByClave(factura.getClave());

      if (facturaExistente.isPresent()) {
        log.info("⚠️ Factura ya registrada anteriormente: {}", factura.getClave());
        return mapearAResponse(facturaExistente.get());
      }

      // 4. Subir archivos a S3
      String rutaXml = subirArchivoS3(
          empresa.getNombreRazonSocial(),
          factura.getTipoDocumento().name(),
          factura.getClave(),
          "xml",
          request.getXmlFile()
      );
      factura.setXmlOriginalPath(rutaXml);

      if (request.getPdfFile() != null && !request.getPdfFile().isEmpty()) {
        String rutaPdf = subirArchivoS3(
            empresa.getNombreRazonSocial(),
            factura.getTipoDocumento().name(),
            factura.getClave(),
            "pdf",
            request.getPdfFile()
        );
        factura.setPdfPath(rutaPdf);
      }

      factura.setFechaRecepcion(LocalDateTime.now());

      // 5. Buscar o crear proveedor
      if (request.isCrearProveedorSiNoExiste()) {
        Proveedor proveedor = buscarOCrearProveedor(factura, empresa, true);
        factura.setProveedor(proveedor);

        // ✅ ACTUALIZAR PLAZO DE CRÉDITO si viene en la factura
        if (factura.getPlazoCredito() != null && factura.getPlazoCredito() > 0) {
          proveedor.setDiasCredito(factura.getPlazoCredito());
          proveedorRepository.save(proveedor);
          log.info("📅 Plazo de crédito actualizado: {} días para proveedor {}, {}",
              factura.getPlazoCredito(), proveedor.getNumeroIdentificacion(), proveedor.getNombreComercial());
        }
      }

      // 6. Guardar como PENDIENTE
      factura.setEstadoInterno(EstadoFacturaRecepcion.PENDIENTE_DECISION);
      factura.setMensajeReceptorEnviado(false);
      factura.setConvertidaCompra(false);

      factura = facturaRecepcionRepository.save(factura);

      log.info("✅ Factura guardada como PENDIENTE. ID: {}", factura.getId());

      return mapearAResponse(factura);

    } catch (IOException e) {
      log.error("Error leyendo archivo XML", e);
      throw new RuntimeException("Error leyendo archivo: " + e.getMessage());
    }
  }

  /**
   * Subir XML y procesar COMPLETAMENTE en un solo paso
   */
  @Transactional
  public FacturaRecepcionResponse subirYProcesarCompleto(SubirXmlRequest request) {
    log.info("Subiendo y procesando XML completamente para empresa: {}, sucursal: {}",
        request.getEmpresaId(), request.getSucursalId());

    try {
      // 1. Validar empresa y sucursal
      Empresa empresa = empresaRepository.findById(request.getEmpresaId())
          .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

      Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
          .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));

      // 2. Leer y parsear XML
      String xmlContent = new String(request.getXmlFile().getBytes(), StandardCharsets.UTF_8);
      FacturaRecepcion factura = xmlParserService.parsearXML(xmlContent, empresa, sucursal);

      // 3. Validar duplicado
      if (facturaRecepcionRepository.existsByClave(factura.getClave())) {
        throw new RuntimeException("Ya existe una factura con esta clave: " + factura.getClave());
      }

      // 4. Subir archivos a S3
      String rutaXml = subirArchivoS3(
          empresa.getNombreRazonSocial(),
          factura.getTipoDocumento().name(),
          factura.getClave(),
          "xml",
          request.getXmlFile()
      );
      factura.setXmlOriginalPath(rutaXml);

      if (request.getPdfFile() != null && !request.getPdfFile().isEmpty()) {
        String rutaPdf = subirArchivoS3(
            empresa.getNombreRazonSocial(),
            factura.getTipoDocumento().name(),
            factura.getClave(),
            "pdf",
            request.getPdfFile()
        );
        factura.setPdfPath(rutaPdf);
      }

      factura.setFechaRecepcion(LocalDateTime.now());

      // 5. BUSCAR O CREAR PROVEEDOR AUTOMÁTICAMENTE
      Proveedor proveedor = buscarOCrearProveedor(factura, empresa, true);
      factura.setProveedor(proveedor);

      // 6. Guardar factura
      factura = facturaRecepcionRepository.save(factura);

      // 7. ACEPTAR AUTOMÁTICAMENTE Y CREAR COMPRA
      if (request.isAceptarAutomaticamente()) {
        aceptarYCrearCompraAutomatico(factura);
      }

      log.info("✅ Factura procesada completamente. ID: {}, Compra ID: {}",
          factura.getId(), factura.getCompra() != null ? factura.getCompra().getId() : "N/A");

      return mapearAResponse(factura);

    } catch (IOException e) {
      log.error("Error leyendo archivo XML", e);
      throw new RuntimeException("Error leyendo archivo: " + e.getMessage());
    }
  }

  /**
   * Aceptar automáticamente y crear compra
   */
  private void aceptarYCrearCompraAutomatico(FacturaRecepcion factura) {
    try {
      // 1. Construir mensaje receptor de aceptación
      String xmlMensaje = construirXmlMensajeReceptor(
          factura,
          TipoMensajeReceptor.ACEPTADO,
          null
      );

      // 2. Enviar a Hacienda
      IdentificacionDTO receptor = IdentificacionDTO.builder()
          .tipoIdentificacion(
              factura.getProveedorTipoIdentificacion().getCodigo()) // asegurar que sea 01/02/03/...
          .numeroIdentificacion(factura.getProveedorIdentificacion())
          .build();

      haciendaClient.enviarMensajeReceptor(
          factura.getEmpresa().getId(),
          xmlMensaje,
          receptor
      );

      // 3. Actualizar estado
      factura.setEstadoInterno(EstadoFacturaRecepcion.ACEPTADA);
      factura.setMensajeReceptorEnviado(true);
      factura.setTipoMensajeReceptor(TipoMensajeReceptor.ACEPTADO);

      // 4. CREAR COMPRA AUTOMÁTICAMENTE
      Compra compra = crearCompraDesdeFactura(factura);
      compra = compraRepository.save(compra);

      // 5. Vincular
      factura.setCompra(compra);
      factura.setConvertidaCompra(true);

      // 6. Actualizar métricas
      actualizarMetricasMensuales(compra);

      log.info("✅ Factura aceptada y compra creada automáticamente. Compra ID: {}", compra.getId());

    } catch (Exception e) {
      log.error("❌ Error en proceso automático, dejando factura como PENDIENTE", e);
      // Si falla, dejar en pendiente para que usuario tome decisión manual
      factura.setEstadoInterno(EstadoFacturaRecepcion.PENDIENTE_DECISION);
    }
  }

  /**
   * Crear Compra desde Factura
   */
  private Compra crearCompraDesdeFactura(FacturaRecepcion factura) {
    log.info("Creando compra desde factura recepción ID: {}", factura.getId());

    // Obtener usuario del contexto de seguridad
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String username = auth.getName();
    Usuario usuario = usuarioRepository.findByEmail(username)
        .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));

    // Crear compra
    Compra compra = new Compra();
    compra.setEmpresa(factura.getEmpresa());
    compra.setSucursal(factura.getSucursal());
    compra.setProveedor(factura.getProveedor());
    compra.setUsuario(usuario);

    // Tipo de documento
    compra.setTipoDocumentoHacienda(factura.getTipoDocumento());
    compra.setClaveHacienda(factura.getClave());
    compra.setNumeroDocumento(factura.getNumeroConsecutivo());

    // Determinar tipo de compra según documento
    if (factura.getTipoDocumento() == TipoDocumento.FACTURA_ELECTRONICA) {
      compra.setTipoCompra(TipoCompra.FACTURA_PROVEEDOR_INSCRITO);
    } else if (factura.getTipoDocumento() == TipoDocumento.TIQUETE_ELECTRONICO) {
      compra.setTipoCompra(TipoCompra.TIQUETE_COMPRA);
    } else {
      compra.setTipoCompra(TipoCompra.FACTURA_PROVEEDOR_INSCRITO);
    }

    // Fechas
    compra.setFechaEmision(factura.getFechaEmision());
    compra.setFechaRecepcion(LocalDateTime.now());

    // Condiciones comerciales
    compra.setCondicionVenta(factura.getCondicionVenta());
    compra.setPlazoCredito(factura.getPlazoCredito());

    // Estado inicial según tipo de mensaje receptor
    if (factura.getTipoMensajeReceptor() == TipoMensajeReceptor.ACEPTADO) {
      compra.setEstado(EstadoCompra.ACEPTADA);
    } else if (factura.getTipoMensajeReceptor() == TipoMensajeReceptor.ACEPTADO_PARCIAL) {
      compra.setEstado(EstadoCompra.ACEPTADA_PARCIAL);
    } else {
      compra.setEstado(EstadoCompra.ACEPTADA); // Default
    }

    // Moneda y tipo de cambio
    if (factura.getMoneda() != null) {
      compra.setMoneda(factura.getMoneda());
    }
    compra.setTipoCambio(factura.getTipoCambio() != null ?
        factura.getTipoCambio() : BigDecimal.ONE);

    // Totales
    compra.setTotalGravado(factura.getTotalGravado());
    compra.setTotalExento(factura.getTotalExento());
    compra.setTotalExonerado(factura.getTotalExonerado());
    compra.setTotalVenta(factura.getTotalVenta());
    compra.setTotalDescuentos(factura.getTotalDescuentos());
    compra.setTotalVentaNeta(factura.getTotalVentaNeta());
    compra.setTotalImpuesto(factura.getTotalImpuesto());
    compra.setTotalOtrosCargos(factura.getTotalOtrosCargos());
    compra.setTotalComprobante(factura.getTotalComprobante());

    // ============ MAPEAR DETALLES ============
    log.debug("Mapeando {} detalles de factura a compra", factura.getDetalles().size());

    for (FacturaRecepcionDetalle detFact : factura.getDetalles()) {
      CompraDetalle detalle = new CompraDetalle();
      detalle.setNumeroLinea(detFact.getNumeroLinea());

      // Códigos
      detalle.setCodigo(detFact.getCodigoComercial());
      detalle.setCodigoCabys(detFact.getCodigoCabys());
      detalle.setDescripcion(detFact.getDetalle());

      // Cantidades y unidades
      detalle.setCantidad(detFact.getCantidad());

      String unidadMedida = detFact.getUnidadMedida() != null
          ? detFact.getUnidadMedida().name()
          : "Unid";
      detalle.setUnidadMedida(unidadMedida);

      String unidadComercial = detFact.getUnidadMedidaComercial();
      if (unidadComercial == null || unidadComercial.trim().isEmpty()) {
        unidadComercial = unidadMedida;  // Usar la unidad medida normal
      }
      detalle.setUnidadMedidaComercial(unidadComercial);

      // Es servicio?
      detalle.setEsServicio(false); // TODO: Determinar según código CABYS

      // Precios
      detalle.setPrecioUnitario(detFact.getPrecioUnitario());
      detalle.setMontoTotal(detFact.getMontoTotal());
      detalle.setMontoDescuento(detFact.getMontoDescuento() != null ?
          detFact.getMontoDescuento() : BigDecimal.ZERO);
      detalle.setSubTotal(detFact.getSubtotal());
      detalle.setBaseImponible(detFact.getBaseImponible());

      // Calcular impuesto total de esta línea
      BigDecimal totalImpuesto = detFact.getImpuestos().stream()
          .map(imp -> imp.getMonto() != null ? imp.getMonto() : BigDecimal.ZERO)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      detalle.setMontoImpuesto(totalImpuesto);

      // Obtener código de tarifa IVA del primer impuesto (si existe)
      if (!detFact.getImpuestos().isEmpty()) {
        FacturaRecepcionDetalleImpuesto primerImpuesto = detFact.getImpuestos().get(0);
        detalle.setCodigoTarifaIVA(primerImpuesto.getCodigoTarifa());
        detalle.setTarifaIVA(primerImpuesto.getTarifa());
      }

      detalle.setMontoTotalLinea(detFact.getMontoTotalLinea());

      // TODO FASE 2: Buscar/crear producto automáticamente
      // detalle.setProducto(buscarOCrearProducto(detFact, factura.getProveedor()));

      // Factor de conversión (si existe en el proveedor)
      detalle.setFactorConversion(BigDecimal.ONE); // Default 1:1

      // Agregar detalle a la compra
      compra.addDetalle(detalle);
    }

    log.info("Compra creada con {} detalles", compra.getDetalles().size());

    return compra;
  }

  /**
   * Mapear FacturaRecepcion a FacturaRecepcionListResponse
   */
  private FacturaRecepcionListResponse toListResponse(FacturaRecepcion fr) {
    return FacturaRecepcionListResponse.builder()
        .id(fr.getId())
        .clave(fr.getClave())
        .numeroConsecutivo(fr.getNumeroConsecutivo())
        .fechaEmision(fr.getFechaEmision())

        // Proveedor
        .proveedorNombre(fr.getProveedorNombre())
        .proveedorIdentificacion(fr.getProveedorIdentificacion())

        // Montos
        .totalComprobante(fr.getTotalComprobante())

        // Estados
        .estadoInterno(fr.getEstadoInterno())
        .mensajeReceptorEnviado(fr.getMensajeReceptorEnviado())
        .convertidaACompra(fr.getConvertidaCompra())

        .build();
  }

  /**
   * Genera un reporte Excel de facturas aceptadas en un rango de fechas
   *
   * @param fechaInicio Fecha inicio del rango (LocalDate)
   * @param fechaFin Fecha fin del rango (LocalDate)
   * @return Archivo Excel como byte array
   */
  public byte[] generarReporteExcel(LocalDate fechaInicio, LocalDate fechaFin) {
    log.info("Generando reporte Excel de facturas aceptadas - Rango: {} a {}", fechaInicio, fechaFin);

    // Convertir LocalDate a LocalDateTime (inicio y fin del día)
    LocalDateTime inicio = fechaInicio.atStartOfDay();
    LocalDateTime fin = fechaFin.atTime(23, 59, 59);

    // Ejecutar query
    List<FacturaRecepcion> facturas = facturaRecepcionRepository.findAceptadasParaReporte(inicio, fin);

    log.info("Se encontraron {} facturas aceptadas en el rango", facturas.size());

    // Transformar a DTO con signo calculado
    List<FacturaRecepcionReporteDTO> datos = facturas.stream()
        .map(this::toReporteDTO)
        .collect(Collectors.toList());

    // Llamar al generador de Excel
    return facturaRecepcionExcelService.generarExcel(datos, fechaInicio, fechaFin);
  }

  /**
   * Mapea una FacturaRecepcion a DTO para reporte
   * Calcula el signo según el tipo de documento
   * Calcula el desglose de IVA por tarifa
   */
  private FacturaRecepcionReporteDTO toReporteDTO(FacturaRecepcion fr) {
    // Determinar signo según tipo
    int signo = calcularSigno(fr.getTipoDocumento());

    // Calcular IVAs por tarifa
    Map<String, BigDecimal> ivasPorTarifa = calcularIVAPorTarifa(fr);

    return FacturaRecepcionReporteDTO.builder()
        .tipoDocumento(getTipoAbreviado(fr.getTipoDocumento()))
        .cedulaEmisor(fr.getProveedorIdentificacion())
        .nombreEmisor(fr.getProveedorNombre())
        .fechaEmision(fr.getFechaEmision())
        .clave(fr.getClave())
        .motivoRespuesta(fr.getMotivoRespuesta() != null ? fr.getMotivoRespuesta() : "")
        // SERVICIOS
        .totalServiciosGravados(fr.getTotalServGravados() != null ? fr.getTotalServGravados() : BigDecimal.ZERO)
        .totalServiciosExentos(fr.getTotalServExentos() != null ? fr.getTotalServExentos() : BigDecimal.ZERO)
        .totalServiciosNoSujetos(fr.getTotalServNoSujeto() != null ? fr.getTotalServNoSujeto() : BigDecimal.ZERO)
        // MERCANCÍAS
        .totalMercanciasGravadas(fr.getTotalMercGravada() != null ? fr.getTotalMercGravada() : BigDecimal.ZERO)
        .totalMercanciasExentas(fr.getTotalMercExenta() != null ? fr.getTotalMercExenta() : BigDecimal.ZERO)
        .totalMercanciasNoSujetas(fr.getTotalMercNoSujeta() != null ? fr.getTotalMercNoSujeta() : BigDecimal.ZERO)
        // TOTALES
        .totalVentaNeta(fr.getTotalVentaNeta())
        .totalImpuesto(fr.getTotalImpuesto())
        // IVA POR TARIFA
        .iva0(ivasPorTarifa.getOrDefault("0", BigDecimal.ZERO))
        .iva1(ivasPorTarifa.getOrDefault("1", BigDecimal.ZERO))
        .iva2(ivasPorTarifa.getOrDefault("2", BigDecimal.ZERO))
        .iva4(ivasPorTarifa.getOrDefault("4", BigDecimal.ZERO))
        .iva8(ivasPorTarifa.getOrDefault("8", BigDecimal.ZERO))
        .iva13(ivasPorTarifa.getOrDefault("13", BigDecimal.ZERO))
        // OTROS TOTALES
        .totalDescuentos(fr.getTotalDescuentos() != null ? fr.getTotalDescuentos() : BigDecimal.ZERO)
        .totalOtrosCargos(fr.getTotalOtrosCargos() != null ? fr.getTotalOtrosCargos() : BigDecimal.ZERO)
        .totalIVADevuelto(fr.getTotalIVADevuelto() != null ? fr.getTotalIVADevuelto() : BigDecimal.ZERO)
        .totalExonerado(fr.getTotalExonerado() != null ? fr.getTotalExonerado() : BigDecimal.ZERO)
        .totalComprobante(fr.getTotalComprobante())
        .signo(signo)
        .build();
  }

  /**
   * Calcula el IVA agrupado por tarifa desde los detalles
   *
   * @param fr Factura de recepción
   * @return Map con key = tarifa (0, 1, 2, 4, 8, 13) y value = monto total
   */
  private Map<String, BigDecimal> calcularIVAPorTarifa(FacturaRecepcion fr) {
    Map<String, BigDecimal> ivasPorTarifa = new HashMap<>();

    // Inicializar todas las tarifas en 0
    ivasPorTarifa.put("0", BigDecimal.ZERO);
    ivasPorTarifa.put("1", BigDecimal.ZERO);
    ivasPorTarifa.put("2", BigDecimal.ZERO);
    ivasPorTarifa.put("4", BigDecimal.ZERO);
    ivasPorTarifa.put("8", BigDecimal.ZERO);
    ivasPorTarifa.put("13", BigDecimal.ZERO);

    // Recorrer detalles
    for (FacturaRecepcionDetalle detalle : fr.getDetalles()) {
      // Recorrer impuestos del detalle
      for (FacturaRecepcionDetalleImpuesto impuesto : detalle.getImpuestos()) {
        // Solo procesar IVA (código 01)
        if ("01".equals(impuesto.getCodigoImpuesto())) {
          // Obtener el monto a usar (considerar exoneraciones)
          BigDecimal montoIVA = impuesto.getMontoExoneracion() != null
              && impuesto.getMontoExoneracion().compareTo(BigDecimal.ZERO) > 0
              ? impuesto.getImpuestoNeto()  // Si hay exoneración, usar neto
              : impuesto.getMonto();         // Si no, usar monto completo

          // Determinar la tarifa según código
          String tarifaKey = mapearCodigoTarifaAKey(impuesto.getCodigoTarifa());

          // Acumular
          BigDecimal acumulado = ivasPorTarifa.getOrDefault(tarifaKey, BigDecimal.ZERO);
          ivasPorTarifa.put(tarifaKey, acumulado.add(montoIVA));
        }
      }
    }

    return ivasPorTarifa;
  }

  /**
   * Mapea el código de tarifa de Hacienda a la key del Map
   *
   * @param codigoTarifa Código de tarifa (01-11)
   * @return Key para el map ("0", "1", "2", "4", "8", "13")
   */
  private String mapearCodigoTarifaAKey(String codigoTarifa) {
    return switch (codigoTarifa) {
      case "02" -> "1";   // 1%
      case "03" -> "2";   // 2%
      case "04", "06" -> "4";  // 4%
      case "07" -> "8";   // 8%
      case "08" -> "13";  // 13% (General)
      default -> "0";     // 0% (01, 05, 10, 11)
    };
  }

  /**
   * Calcula el signo para el cálculo de totales
   * Facturas y Tiquetes suman (+1)
   * Notas de Crédito restan (-1)
   *
   * @param tipo Tipo de documento
   * @return 1 para sumar, -1 para restar
   */
  private int calcularSigno(TipoDocumento tipo) {
    return switch (tipo) {
      case FACTURA_ELECTRONICA, TIQUETE_ELECTRONICO, NOTA_DEBITO -> 1;
      case NOTA_CREDITO -> -1;
      default -> 1;
    };
  }

  /**
   * Obtiene la abreviación del tipo de documento
   *
   * @param tipo Tipo de documento
   * @return Abreviación (FE, TE, NC, ND)
   */
  private String getTipoAbreviado(TipoDocumento tipo) {
    return switch (tipo) {
      case FACTURA_ELECTRONICA -> "FE";
      case TIQUETE_ELECTRONICO -> "TE";
      case NOTA_CREDITO -> "NC";
      case NOTA_DEBITO -> "ND";
      default -> tipo.name();
    };
  }
}