package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.auth.Contexto;
import com.snnsoluciones.backnathbitpos.dto.facturarecepcion.*;
import com.snnsoluciones.backnathbitpos.dto.facturarecepcion.DecisionMensajeRequest.TipoDecision;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.factura.EstadoFacturaRecepcion;
import com.snnsoluciones.backnathbitpos.enums.factura.TipoMensajeReceptor;
import com.snnsoluciones.backnathbitpos.enums.mh.EstadoCompra;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoCompra;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.HaciendaClient;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.scheduler.FacturaRecepcionXMLParserService;
import com.snnsoluciones.backnathbitpos.sign.SignerService;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

  private final FacturaRecepcionXMLParserService xmlParserService;
  private final StorageService storageService;
  private final HaciendaClient haciendaClient;
  private final SignerService signerService;

  public Optional<FacturaRecepcion> buscarPorId(Long id) {
    return facturaRecepcionRepository.findById(id);
  }


  /**
   * Subir XML y parsear factura recibida
   */
  @Transactional
  public FacturaRecepcionResponse subirYParsearXml(SubirXmlRequest request) {
    log.info("Subiendo XML para empresa: {}, sucursal: {}",
        request.getEmpresaId(), request.getSucursalId());

    try {
      // 1. Validar empresa y sucursal
      Empresa empresa = empresaRepository.findById(request.getEmpresaId())
          .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

      Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
          .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));

      // 2. Leer contenido del XML
      String xmlContent = new String(request.getXmlFile().getBytes(), StandardCharsets.UTF_8);

      // 3. Parsear XML a entidad
      FacturaRecepcion factura = xmlParserService.parsearXML(xmlContent, empresa, sucursal);

      // 4. Validar que no exista duplicada
      if (facturaRecepcionRepository.existsByClave(factura.getClave())) {
        throw new RuntimeException("Ya existe una factura con esta clave: " + factura.getClave());
      }

      // 5. Subir XML a S3
      String rutaXml = subirArchivoS3(
          empresa.getNombreRazonSocial(),
          factura.getTipoDocumento().name(),
          factura.getClave(),
          "xml",
          request.getXmlFile()
      );
      factura.setXmlOriginalPath(rutaXml);

      // 6. Subir PDF si viene
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

      // 7. Fecha de recepción
      factura.setFechaRecepcion(LocalDateTime.now());

      // 8. Buscar o crear proveedor
      Proveedor proveedor = buscarOCrearProveedor(factura, empresa,
          request.isCrearProveedorSiNoExiste());
      if (proveedor != null) {
        factura.setProveedor(proveedor);
      }

      // 9. Guardar
      factura = facturaRecepcionRepository.save(factura);

      log.info("Factura recepción guardada exitosamente. ID: {}, Clave: {}",
          factura.getId(), factura.getClave());

      return mapearAResponse(factura);

    } catch (IOException e) {
      log.error("Error leyendo archivo XML", e);
      throw new RuntimeException("Error leyendo archivo: " + e.getMessage());
    }
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
        throw new RuntimeException("El monto de IVA aceptado es obligatorio para aceptación parcial");
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

      log.info("Tipo de mensaje receptor: {}", tipoMensaje);
      // 1. Construir XML mensaje receptor
      String xmlSinFirmar = construirXmlMensajeReceptor(factura, tipoMensaje, request);

      // 2. Firmar XML (si aplica)
      byte[] xmlFirmado = signerService.signXmlForEmpresa(
          xmlSinFirmar.getBytes(StandardCharsets.UTF_8),
          factura.getEmpresa().getId(),
          TipoDocumento.MENSAJE_RECEPTOR  // 👈 Usar el enum correcto
      );

      String xmlMensajeFirmado = new String(xmlFirmado, StandardCharsets.UTF_8);

      // 3. Enviar a Hacienda
      String respuestaHacienda = haciendaClient.enviarMensajeReceptor(
          factura.getEmpresa().getId(),
          xmlMensajeFirmado  // 👈 Enviar el firmado
      );

      // 4. Actualizar factura
      factura.setMensajeReceptorEnviado(true);
      factura.setTipoMensajeReceptor(tipoMensaje);

      // 5. Guardar
      factura = facturaRecepcionRepository.save(factura);

      log.info("✅ Mensaje receptor enviado exitosamente. Factura ID: {}", factura.getId());

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
      proveedor.setNombreComercial(factura.getProveedorNombreComercial()); // 👈 PUEDE SER NULL
      proveedor.setEmail(factura.getProveedorEmail());
      proveedor.setTelefono(factura.getProveedorTelefono());
      proveedor.setActivo(true);

      proveedor = proveedorRepository.save(proveedor);  // 👈 AQUÍ FALLA
      log.info("Proveedor creado automáticamente: {}", proveedor.getNumeroIdentificacion());
    }

    return proveedor;
  }

  private String construirXmlMensajeReceptor(FacturaRecepcion factura,
      TipoMensajeReceptor tipo,
      DecisionMensajeRequest request) {

    // ✅ Usar ZonedDateTime con zona horaria de Costa Rica
    ZonedDateTime fechaEmision = ZonedDateTime.now(ZoneId.of("America/Costa_Rica"));
    String fechaFormateada = fechaEmision.format(
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
    );

    // Determinar código de mensaje según tipo
    String codigoMensaje = tipo.getCodigo(); // 1=Aceptado, 2=Parcial, 3=Rechazado

    // Detalle del mensaje (obligatorio para rechazo y parcial)
    String detalleMensaje = "";
    if (tipo == TipoMensajeReceptor.RECHAZADO || tipo == TipoMensajeReceptor.ACEPTADO_PARCIAL) {
      detalleMensaje = request != null && request.getRazon() != null
          ? request.getRazon()
          : "Sin detalle";
    }

    // Montos para aceptación parcial
    BigDecimal montoTotalImpuesto = BigDecimal.ZERO;
    BigDecimal totalFactura = BigDecimal.ZERO;

    if (tipo == TipoMensajeReceptor.ACEPTADO_PARCIAL && request != null) {
      montoTotalImpuesto = Optional.ofNullable(request.getMontoIvaAceptado())
          .orElse(factura.getTotalImpuesto());
      totalFactura = Optional.ofNullable(request.getMontoAceptado())
          .orElse(factura.getTotalComprobante());
    }

    // Construir XML según estructura oficial de Hacienda
    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xml.append("<MensajeReceptor xmlns=\"https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/mensajeReceptor\" ");
    xml.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
    xml.append("xsi:schemaLocation=\"https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/mensajeReceptor MensajeReceptor_4.4.xsd\">\n");

    // Clave del comprobante (50 dígitos)
    xml.append("  <Clave>").append(factura.getClave()).append("</Clave>\n");

    // Número de cédula del emisor (del XML original)
    xml.append("  <NumeroCedulaEmisor>").append(factura.getProveedorIdentificacion()).append("</NumeroCedulaEmisor>\n");

    // Fecha y hora de emisión del mensaje
    xml.append("  <FechaEmisionDoc>").append(fechaFormateada).append("</FechaEmisionDoc>\n");

    // Código del mensaje (1, 2 o 3)
    xml.append("  <Mensaje>").append(codigoMensaje).append("</Mensaje>\n");

    // Detalle del mensaje (obligatorio para rechazo y parcial)
    if (tipo == TipoMensajeReceptor.RECHAZADO || tipo == TipoMensajeReceptor.ACEPTADO_PARCIAL) {
      xml.append("  <DetalleMensaje>").append(escapeXml(detalleMensaje)).append("</DetalleMensaje>\n");
    }

    // Montos (solo para aceptación parcial)
    if (tipo == TipoMensajeReceptor.ACEPTADO_PARCIAL) {
      xml.append("  <MontoTotalImpuesto>").append(montoTotalImpuesto).append("</MontoTotalImpuesto>\n");
      xml.append("  <TotalFactura>").append(totalFactura).append("</TotalFactura>\n");
    }

    xml.append("</MensajeReceptor>");

    return xml.toString();
  }

  /**
   * Escapar caracteres especiales XML
   */
  private String escapeXml(String text) {
    if (text == null) return "";
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
      haciendaClient.enviarMensajeReceptor(
          factura.getEmpresa().getId(),
          xmlMensaje
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
      detalle.setUnidadMedida(detFact.getUnidadMedida() != null ?
          detFact.getUnidadMedida().name() : "Unid");
      detalle.setUnidadMedidaComercial(detFact.getUnidadMedidaComercial());

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
}