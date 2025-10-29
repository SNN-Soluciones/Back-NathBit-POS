//package com.snnsoluciones.backnathbitpos.scheduler;
//
//import com.snnsoluciones.backnathbitpos.dto.email.EmailFacturaDto;
//import com.snnsoluciones.backnathbitpos.entity.Empresa;
//import com.snnsoluciones.backnathbitpos.entity.Factura;
//import com.snnsoluciones.backnathbitpos.entity.FacturaBitacora;
//import com.snnsoluciones.backnathbitpos.enums.EstadoEmail;
//import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
//import com.snnsoluciones.backnathbitpos.enums.facturacion.TipoArchivoFactura;
//import com.snnsoluciones.backnathbitpos.enums.mh.AmbienteHacienda;
//import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
//import com.snnsoluciones.backnathbitpos.integrations.hacienda.HaciendaClient;
//import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.ConsultaEstadoResponse;
//import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.HaciendaTokenResponse;
//import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.IdentificacionDTO;
//import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.RecepcionRequest;
//import com.snnsoluciones.backnathbitpos.repository.EmailAuditLogRepository;
//import com.snnsoluciones.backnathbitpos.repository.FacturaBitacoraRepository;
//import com.snnsoluciones.backnathbitpos.repository.FacturaRepository;
//import com.snnsoluciones.backnathbitpos.service.EmailService;
//import com.snnsoluciones.backnathbitpos.service.ImapService;
//import com.snnsoluciones.backnathbitpos.service.StorageService;
//import com.snnsoluciones.backnathbitpos.service.pdf.FacturaPdfService;
//import com.snnsoluciones.backnathbitpos.sign.SignerService;
//import com.snnsoluciones.backnathbitpos.util.ByteArrayMultipartFile;
//import com.snnsoluciones.backnathbitpos.util.S3PathBuilder;
//import jakarta.persistence.EntityManager;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.http.HttpStatus;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Propagation;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.transaction.support.TransactionSynchronization;
//import org.springframework.transaction.support.TransactionSynchronizationManager;
//import org.springframework.web.client.HttpClientErrorException;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.nio.charset.StandardCharsets;
//import java.time.LocalDateTime;
//import java.time.ZonedDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.Base64;
//import java.util.List;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class FacturaElectronicaJob {
//
//  private final FacturaBitacoraRepository bitacoraRepository;
//  private final FacturaRepository facturaRepository;
//  private final FacturaXMLGeneratorService xmlGeneratorService;
//  private final SignerService signerService;
//  private final HaciendaClient haciendaService;
//  private final FacturaPdfService pdfService;
//  private final EmailService emailService;
//  private final StorageService storageService;
//  private final S3PathBuilder s3PathBuilder;
//  private final EmailAuditLogRepository emailAuditLogRepository;
//  private final ImapService imapService;
//  private final EntityManager entityManager;
//
//  @org.springframework.beans.factory.annotation.Autowired
//  @org.springframework.context.annotation.Lazy
//  private FacturaElectronicaJob self;
//
//  // Configuración
//  private static final int MAX_FACTURAS_POR_CICLO = 6;
//  private static final int MAX_INTENTOS = 3;
//
//  @Scheduled(fixedDelay = 60000, initialDelay = 10000)
//  // IMPORTANTE: NO transaccional aquí
//  public void procesarFacturasPendientes() {
//    log.info("⏳ Iniciando job de procesamiento de facturas electrónicas...");
//
//    List<FacturaBitacora> pendientes = bitacoraRepository.findFacturasPendientesProcesar(
//        LocalDateTime.now(),
//        PageRequest.of(0, MAX_FACTURAS_POR_CICLO)
//    );
//
//    if (pendientes.isEmpty()) {
//      log.debug("No hay facturas pendientes para procesar");
//      return;
//    }
//
//    int exitosas = 0;
//    int fallidas = 0;
//
//    log.info("Se encontraron {} facturas pendientes", pendientes.size());
//
//    for (FacturaBitacora bitacora : pendientes) {
//      try {
//        self.procesarFacturaFlow(bitacora);// cada factura en su propia transacción
//        exitosas++;
//        bitacoraRepository.flush();
//      } catch (Exception e) {
//        fallidas++;
//        log.error("❌ Error procesando factura {}: {}", bitacora.getClave(), e.getMessage(), e);
//        try {
//          manejarErrorTransactional(bitacora, e); // asegurar persistencia del estado de error
//        } catch (Exception ee) {
//          log.error("❌ Error al persistir el manejo de error para {}: {}", bitacora.getClave(), ee.getMessage(), ee);
//        }
//        entityManager.clear();
//        log.debug("⚠️ Error manejado y contexto limpiado");
//      }
//    }
//
//    log.info("🏁 Job finalizado - Exitosas: {}, Fallidas: {}", exitosas, fallidas);
//  }
//
//  /**
//   * Flow completo de una factura: Validar -> XML -> Firmar -> Enviar -> Poll -> Guardar respuesta -> PDF -> Email
//   * Corre en transacción independiente.
//   */
//  @Transactional(propagation = Propagation.REQUIRES_NEW)
//  protected void procesarFacturaFlow(FacturaBitacora bitacora) throws Exception {
//    log.info("➡️ Procesando factura {} (intento #{})", bitacora.getClave(), bitacora.getIntentos() + 1);
//
//    // 1) Marcar intento
//    bitacora.setIntentos(bitacora.getIntentos() + 1);
//    bitacora.setEstado(EstadoBitacora.PROCESANDO);
//    bitacoraRepository.save(bitacora);
//
//    Factura factura = facturaRepository.findByIdFetchEmpresa(bitacora.getFacturaId())
//        .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + bitacora.getFacturaId()));
//    final Empresa empresa = factura.getSucursal().getEmpresa();
//    final String empresaNombre = empresa.getNombreComercial();
//    final boolean produccion = empresa.getConfigHacienda().getAmbiente() == AmbienteHacienda.PRODUCCION;
//
//    // 0) Validaciones previas 4.4 (corta de raíz si falta info)
//    log.info("[0/6] Validando requisitos mínimos 4.4...");
//    validarFacturaPreXML(factura);
//
//    // 2) Generar XML
//    log.info("[1/6] Generando XML...");
//    String xml = xmlGeneratorService.generarXML(factura.getId());
//    String xmlPath = s3PathBuilder.buildXmlPath(factura, TipoArchivoFactura.XML_UNSIGNED, empresaNombre);
//    storageService.uploadFile(
//        createMultipartFile(xml.getBytes(StandardCharsets.UTF_8),
//            "factura_" + factura.getClave() + ".xml", "text/xml"),
//        xmlPath
//    );
//    bitacora.setXmlPath(xmlPath);
//    bitacoraRepository.save(bitacora);
//
//    // 3) Firmar XML
//    log.info("[2/6] Firmando XML...");
//    byte[] xmlUnsigned = storageService.downloadFileAsBytes(xmlPath);
//    byte[] xmlFirmado = signerService.signXmlForEmpresa(xmlUnsigned, empresa.getId(), factura.getTipoDocumento());
//
//    String xmlFirmadoPath = s3PathBuilder.buildXmlPath(factura, TipoArchivoFactura.XML_SIGNED, empresaNombre);
//    storageService.uploadFile(
//        createMultipartFile(xmlFirmado, "factura_" + factura.getClave() + "_firmado.xml", "text/xml"),
//        xmlFirmadoPath
//    );
//    bitacora.setXmlFirmadoPath(xmlFirmadoPath);
//    bitacoraRepository.save(bitacora);
//
//    // 4) Enviar a Hacienda
//    log.info("[3/6] Enviando a Hacienda...");
//    HaciendaTokenResponse token = getToken(empresa, produccion);
//
//    // receptor opcional (no para Tiquete 04)
//    IdentificacionDTO receptor = null;
//    if (!"04".equals(factura.getTipoDocumento().getCodigo()) &&
//        factura.getCliente() != null && factura.getCliente().getNumeroIdentificacion() != null) {
//      receptor = IdentificacionDTO.builder()
//          .tipoIdentificacion(factura.getCliente().getTipoIdentificacion().getCodigo())
//          .numeroIdentificacion(factura.getCliente().getNumeroIdentificacion())
//          .build();
//    }
//
//    RecepcionRequest req = RecepcionRequest.builder()
//        .clave(factura.getClave())
//        .fecha(formatearFechaHacienda(factura.getFechaEmision()))
//        .emisor(IdentificacionDTO.builder()
//            .tipoIdentificacion(empresa.getTipoIdentificacion().getCodigo())
//            .numeroIdentificacion(empresa.getIdentificacion())
//            .build())
//        .receptor(receptor)
//        .comprobanteXml(Base64.getEncoder().encodeToString(xmlFirmado))
//        .build();
//
//    try {
//      haciendaService.postRecepcion(token.getAccessToken(), produccion, req);
//      factura.setEstado(EstadoFactura.ENVIADA);
//      bitacora.setHaciendaMensaje("Enviada a MH");
//      facturaRepository.save(factura);
//      bitacoraRepository.save(bitacora);
//    } catch (HttpClientErrorException ex) {
//      log.warn("[MH] POST /recepcion falló {} {}. Intentando reconsulta por clave {}...",
//          ex.getStatusCode(), ex.getStatusText(), factura.getClave());
//
//      ConsultaEstadoResponse estadoTrasPostFallido =
//          consultarEstadoConRefreshSi400(empresa, produccion, factura.getClave(), token.getAccessToken());
//
//      if (estadoTrasPostFallido != null) {
//        String ind = safeUpper(estadoTrasPostFallido.getIndEstado());
//        if ("ACEPTADO".equals(ind) || "RECHAZADO".equals(ind)) {
//          guardarRespuestaDeHacienda(bitacora, factura, estadoTrasPostFallido, empresaNombre);
//          if ("ACEPTADO".equals(ind)) {
//            factura.setEstado(EstadoFactura.ACEPTADA);
//            bitacora.setEstado(EstadoBitacora.ACEPTADA);
//            bitacora.setHaciendaMensaje("Aceptada por MH (tras POST fallido)");
//            facturaRepository.save(factura);
//            bitacoraRepository.save(bitacora);
//
//            // Sólo Factura (01) genera PDF/Email
//            if ("01".equals(factura.getTipoDocumento().getCodigo())) {
//              programarEnvioEmailPostCommit(bitacora, factura);
//            } else {
//              log.info("Tipo {} aceptado (tras POST fallido): no PDF/Email.", factura.getTipoDocumento().getCodigo());
//            }
//            log.info("✅ Factura {} completada con estado {}", factura.getClave(), factura.getEstado());
//            return;
//          } else {
//            // RECHAZADO
//            factura.setEstado(EstadoFactura.RECHAZADA);
//            bitacora.setEstado(EstadoBitacora.RECHAZADA);
//            bitacora.setHaciendaMensaje(estadoTrasPostFallido.getDetalleMensaje() != null
//                ? estadoTrasPostFallido.getDetalleMensaje()
//                : "Rechazada por MH (tras POST fallido)");
//            facturaRepository.save(factura);
//            bitacoraRepository.save(bitacora);
//            log.info("✅ Factura {} marcada RECHAZADA (tras POST fallido)", factura.getClave());
//            return;
//          }
//        }
//      }
//
//      // No terminal → marcar para retry
//      throw ex; // que lo capture el caller y dispare manejo de error transaccional
//    }
//
//    // 5) Poll
//    log.info("[4/6] Consultando estado en MH...");
//    ConsultaEstadoResponse estado = pollEstadoHacienda(token.getAccessToken(), produccion, factura.getClave(), 6, 10);
//
//    if (estado != null) {
//      guardarRespuestaDeHacienda(bitacora, factura, estado, empresaNombre);
//
//      String ind = safeUpper(estado.getIndEstado());
//      if ("ACEPTADO".equals(ind)) {
//        factura.setEstado(EstadoFactura.ACEPTADA);
//        bitacora.setEstado(EstadoBitacora.ACEPTADA);
//        bitacora.setHaciendaMensaje("Aceptada por MH");
//      } else if ("RECHAZADO".equals(ind) || "ERROR".equals(ind)) {
//        factura.setEstado(EstadoFactura.RECHAZADA);
//        bitacora.setEstado(EstadoBitacora.RECHAZADA);
//        bitacora.setHaciendaMensaje(estado.getDetalleMensaje() != null
//            ? estado.getDetalleMensaje()
//            : "Rechazada/Error en MH");
//      } else {
//        factura.setEstado(EstadoFactura.ENVIADA); // RECIBIDO/PROCESANDO
//        bitacora.setHaciendaMensaje("Pendiente en MH: " + ind);
//      }
//
//      facturaRepository.save(factura);
//      bitacoraRepository.save(bitacora);
//    } else {
//      log.warn("No se pudo obtener estado definitivo de Hacienda para factura {}", factura.getClave());
//      bitacora.setHaciendaMensaje("Timeout esperando respuesta de Hacienda");
//      bitacoraRepository.save(bitacora);
//    }
//
//    // 6) PDF + Email sólo si ACEPTADA y tipo 01
//    if (factura.getEstado() == EstadoFactura.ACEPTADA && "01".equals(factura.getTipoDocumento().getCodigo())) {
//      if (yaSeEnvioEmail(factura.getClave(), factura.getEmailReceptor())) {
//        log.info("⏭️ Email ya enviado...");
//        bitacora.setEstado(EstadoBitacora.ACEPTADA);
//        bitacoraRepository.save(bitacora);
//        log.info("✅ Factura {} completada (email ya enviado)", factura.getClave());
//        return;
//      }
//      programarEnvioEmailPostCommit(bitacora, factura);
//      bitacora.setEstado(EstadoBitacora.ACEPTADA);
//      bitacoraRepository.save(bitacora);
//    } else if (factura.getEstado() == EstadoFactura.ACEPTADA) {
//      log.info("Tipo {} aceptado: no PDF/Email.", factura.getTipoDocumento().getCodigo());
//      bitacora.setEstado(EstadoBitacora.ACEPTADA);
//      bitacoraRepository.save(bitacora);
//    }
//
//    log.info("✅ Factura {} completada con estado {}", factura.getClave(), factura.getEstado());
//  }
//
//  @Transactional(propagation = Propagation.REQUIRES_NEW)
//  protected void manejarErrorTransactional(FacturaBitacora bitacora, Exception e) {
//    manejarError(bitacora, e);
//  }
//
//  /**
//   * Encola el envío del correo para ejecutarse sólo DESPUÉS del commit de la transacción actual.
//   */
//  private void programarEnvioEmailPostCommit(FacturaBitacora bitacora, Factura factura) {
//    // Preparar datos dentro de la transacción
//    final byte[] pdfBytes;
//    try {
//      log.info("[5/6] Generando PDF...");
//      pdfBytes = pdfService.generarFacturaCarta(factura.getClave());
//    } catch (Exception ex) {
//      log.error("❌ Error generando PDF: {}", ex.getMessage(), ex);
//      return;
//    }
//
//    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
//      @Override
//      public void afterCommit() {
//        try {
//          log.info("[6/6] Enviando correo (post-commit)...");
//          enviarEmail(bitacora, factura, pdfBytes);
//        } catch (Exception mailEx) {
//          log.error("❌ Error enviando email post-commit para factura {}: {}", factura.getClave(), mailEx.getMessage(), mailEx);
//        }
//      }
//    });
//  }
//
//  /**
//   * Intenta GET /recepcion/{clave}. Si 400, renueva token y reintenta 1 vez.
//   */
//  private ConsultaEstadoResponse consultarEstadoConRefreshSi400(Empresa empresa, boolean produccion, String clave, String tokenActual) {
//    try {
//      ConsultaEstadoResponse r = haciendaService.getEstado(tokenActual, !produccion, clave);
//      if (r != null && r.getIndEstado() != null) {
//        log.info("[MH] Reconsulta tras POST fallido OK: indEstado={}", r.getIndEstado());
//        return r;
//      }
//    } catch (HttpClientErrorException ex) {
//      if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
//        log.warn("[MH] GET estado devolvió 400. Renovando token y reintentando (clave {})...", clave);
//        try {
//          HaciendaTokenResponse nuevo = getToken(empresa, produccion);
//          ConsultaEstadoResponse r2 = haciendaService.getEstado(nuevo.getAccessToken(), !produccion, clave);
//          if (r2 != null && r2.getIndEstado() != null) {
//            log.info("[MH] Reintento GET con token fresco OK: indEstado={}", r2.getIndEstado());
//            return r2;
//          }
//        } catch (HttpClientErrorException ex2) {
//          log.warn("[MH] Segundo GET estado falló {}: {}", ex2.getStatusCode(), ex2.getMessage());
//        }
//      } else {
//        log.warn("[MH] GET estado falló {}: {}", ex.getStatusCode(), ex.getMessage());
//      }
//    } catch (Exception e) {
//      log.warn("[MH] Error consultando estado tras POST fallido: {}", e.getMessage());
//    }
//    return null;
//  }
//
//  private HaciendaTokenResponse getToken(Empresa empresa, boolean produccion) {
//    return haciendaService.getToken(
//        com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.HaciendaAuthParams.builder()
//            .empresaId(empresa.getId())
//            .username(empresa.getConfigHacienda().getUsuarioHacienda())
//            .password(empresa.getConfigHacienda().getClaveHacienda())
//            .clientId(produccion ? "api-prod" : "api-test")
//            .sandbox(!produccion)
//            .build()
//    );
//  }
//
//  /**
//   * Poll corto: consulta /recepcion/{clave} hasta estado terminal o agotar intentos
//   */
//  private ConsultaEstadoResponse pollEstadoHacienda(String accessToken, boolean produccion, String clave, int maxIntentos, int sleepSegs) {
//    for (int i = 0; i < maxIntentos; i++) {
//      ConsultaEstadoResponse r = haciendaService.getEstado(accessToken, !produccion, clave);
//      String e = r != null ? safeUpper(r.getIndEstado()) : null;
//      if ("ACEPTADO".equals(e) || "RECHAZADO".equals(e)) {
//        return r;
//      }
//      try {
//        Thread.sleep(sleepSegs * 1000L);
//      } catch (InterruptedException ie) {
//        Thread.currentThread().interrupt();
//        return null;
//      }
//    }
//    return null;
//  }
//
//  /**
//   * Guarda la respuesta XML (si vino en base64 o bytes) en storage y actualiza bitácora
//   */
//  private void guardarRespuestaDeHacienda(FacturaBitacora bitacora, Factura factura, ConsultaEstadoResponse estado, String empresaNombre) {
//    try {
//      byte[] respuestaBytes = null;
//      if (estado.getRespuestaXmlBase64() != null && !estado.getRespuestaXmlBase64().isBlank()) {
//        try {
//          respuestaBytes = Base64.getDecoder().decode(estado.getRespuestaXmlBase64());
//        } catch (IllegalArgumentException badB64) {
//          respuestaBytes = estado.getRespuestaXmlBase64().getBytes(StandardCharsets.UTF_8);
//        }
//      }
//      if (respuestaBytes != null) {
//        String respuestaPath = s3PathBuilder.buildXmlPath(factura, TipoArchivoFactura.XML_RESPUESTA, empresaNombre);
//        MultipartFile respuestaFile = createMultipartFile(
//            respuestaBytes, "factura_" + factura.getClave() + "_respuesta.xml", "application/xml");
//        storageService.uploadFile(respuestaFile, respuestaPath);
//        bitacora.setXmlRespuestaPath(respuestaPath);
//      }
//    } catch (Exception ex) {
//      log.warn("No se pudo guardar el XML de respuesta para clave {}: {}", factura.getClave(), ex.getMessage());
//    }
//  }
//
//  private static String safeUpper(String s) { return s == null ? null : s.trim().toUpperCase(); }
//
//  private void enviarEmail(FacturaBitacora bitacora, Factura factura, byte[] pdfBytes) {
//    try {
//      if (factura.getCliente() != null &&
//          factura.getEmailReceptor() != null &&
//          !factura.getEmailReceptor().isEmpty()) {
//
//        byte[] xmlFirmadoBytes = null;
//        if (bitacora.getXmlFirmadoPath() != null) {
//          xmlFirmadoBytes = storageService.downloadFileAsBytes(bitacora.getXmlFirmadoPath());
//        }
//        byte[] respuestaHaciendaBytes = null;
//        if (bitacora.getXmlRespuestaPath() != null) {
//          respuestaHaciendaBytes = storageService.downloadFileAsBytes(bitacora.getXmlRespuestaPath());
//        }
//
//        EmailFacturaDto dto = EmailFacturaDto.builder()
//            .facturaId(factura.getId())
//            .clave(factura.getClave())
//            .consecutivo(factura.getConsecutivo())
//            .emailDestino(factura.getEmailReceptor())
//            .tipoDocumento(factura.getTipoDocumento().getDescripcion())
//            .nombreComercial(factura.getSucursal().getEmpresa().getNombreComercial())
//            .razonSocial(factura.getSucursal().getEmpresa().getNombreRazonSocial())
//            .cedulaJuridica(factura.getSucursal().getEmpresa().getIdentificacion())
//            .fechaEmision(factura.getFechaEmision().toString())
//            .logoUrl(factura.getSucursal().getEmpresa().getLogoUrl())
//            .pdfBytes(pdfBytes)
//            .xmlFirmadoBytes(xmlFirmadoBytes)
//            .respuestaHaciendaBytes(respuestaHaciendaBytes)
//            .build();
//
//        emailService.enviarFacturaElectronica(dto);
//        log.info("📧 Email enviado a: {}", factura.getEmailReceptor());
//      }
//    } catch (Exception e) {
//      log.error("❌ Error enviando email para factura {}: {}", factura.getClave(), e.getMessage());
//    }
//  }
//
//  private void manejarError(FacturaBitacora bitacora, Exception e) {
//    log.error("Error procesando factura {}: {}", bitacora.getClave(), e.getMessage());
//    bitacora.setUltimoError(e.getMessage());
//
//    if (bitacora.getIntentos() >= MAX_INTENTOS) {
//      bitacora.setEstado(EstadoBitacora.ERROR);
//      log.error("Factura {} marcada como ERROR después de {} intentos", bitacora.getClave(), MAX_INTENTOS);
//    } else {
//      bitacora.setEstado(EstadoBitacora.PENDIENTE);
//      bitacora.setProximoIntento(calcularProximoIntento(bitacora.getIntentos()));
//      log.warn("Factura {} será reintentada en {}", bitacora.getClave(), bitacora.getProximoIntento());
//    }
//    bitacoraRepository.save(bitacora);
//  }
//
//  private LocalDateTime calcularProximoIntento(int intentos) {
//    int minutos = switch (intentos) { case 1 -> 5; case 2 -> 15; default -> 30; };
//    return LocalDateTime.now().plusMinutes(minutos);
//  }
//
//  private MultipartFile createMultipartFile(byte[] content, String fileName, String contentType) {
//    return new ByteArrayMultipartFile(content, "file", fileName, contentType);
//  }
//
//  private static final DateTimeFormatter HACIENDA_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
//  private String formatearFechaHacienda(String fechaEmision) {
//    ZonedDateTime zdt = ZonedDateTime.parse(fechaEmision);
//    return zdt.format(HACIENDA_FMT);
//  }
//
//  /**
//   * Valida requisitos mínimos de 4.4 antes de generar XML.
//   */
//  private void validarFacturaPreXML(Factura factura) {
//    Empresa emp = factura.getSucursal().getEmpresa();
//    if (emp == null || emp.getProvincia() == null) {
//      throw new IllegalStateException("Emisor/Ubicación incompleta: faltan datos obligatorios en 4.4");
//    }
//    if (emp.getCanton() == null || emp.getDistrito() == null || emp.getOtrasSenas() == null || emp.getOtrasSenas()
//        .isBlank()) {
//      throw new IllegalStateException("Emisor/Ubicación incompleta: Provincia, Cantón, Distrito y OtrasSenas son obligatorios en 4.4");
//    }
//  }
//
//  /**
//   * Doble validación: BD primero, IMAP como respaldo.
//   */
//  private boolean yaSeEnvioEmail(String clave, String emailDestino) {
//    if (clave == null || emailDestino == null || emailDestino.isBlank()) return false;
//
//    boolean existeEnBD = emailAuditLogRepository.existsByClaveAndEmailDestinoAndEstado(
//        clave, emailDestino, EstadoEmail.ENVIADO);
//    if (existeEnBD) {
//      log.info("✅ Email ya enviado (verificado en BD): {} -> {}", clave, emailDestino);
//      return true;
//    }
//
//    log.info("🔍 Email no encontrado en BD, verificando en carpeta Sent vía IMAP...");
//    try {
//      boolean existeEnIMAP = imapService.emailExisteEnEnviados(clave, emailDestino);
//      if (existeEnIMAP) {
//        log.warn("⚠️ Email encontrado en IMAP pero NO en BD - posible inconsistencia para: {} -> {}", clave, emailDestino);
//      }
//      return existeEnIMAP;
//    } catch (Exception e) {
//      log.error("❌ Error consultando IMAP, asumiendo NO enviado: {}", e.getMessage());
//      return false;
//    }
//  }
//}