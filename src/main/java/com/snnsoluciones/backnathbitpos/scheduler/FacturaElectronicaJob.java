//package com.snnsoluciones.backnathbitpos.scheduler;
//
//import com.snnsoluciones.backnathbitpos.dto.email.EmailFacturaDto;
//import com.snnsoluciones.backnathbitpos.entity.Empresa;
//import com.snnsoluciones.backnathbitpos.entity.Factura;
//import com.snnsoluciones.backnathbitpos.entity.FacturaBitacora;
//import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
//import com.snnsoluciones.backnathbitpos.enums.facturacion.TipoArchivoFactura;
//import com.snnsoluciones.backnathbitpos.enums.mh.AmbienteHacienda;
//import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
//import com.snnsoluciones.backnathbitpos.integrations.hacienda.HaciendaClient;
//import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.ConsultaEstadoResponse;
//import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.HaciendaAuthParams;
//import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.HaciendaTokenResponse;
//import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.IdentificacionDTO;
//import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.RecepcionRequest;
//import com.snnsoluciones.backnathbitpos.repository.FacturaBitacoraRepository;
//import com.snnsoluciones.backnathbitpos.repository.FacturaRepository;
//import com.snnsoluciones.backnathbitpos.service.EmailService;
//import com.snnsoluciones.backnathbitpos.service.StorageService;
//import com.snnsoluciones.backnathbitpos.service.pdf.FacturaPdfService;
//import com.snnsoluciones.backnathbitpos.sign.SignerService;
//import com.snnsoluciones.backnathbitpos.util.ByteArrayMultipartFile;
//import com.snnsoluciones.backnathbitpos.util.S3PathBuilder;
//import jakarta.transaction.Transactional;
//import java.nio.charset.StandardCharsets;
//import java.time.LocalDateTime;
//import java.time.ZonedDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.Base64;
//import java.util.List;
//import java.util.Optional;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.http.HttpStatus;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.HttpClientErrorException;
//import org.springframework.web.multipart.MultipartFile;
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
//
//  // Configuración
//  private static final int MAX_FACTURAS_POR_CICLO = 10;
//  private static final int MAX_INTENTOS = 3;
//
//  @Scheduled(fixedDelay = 60000, initialDelay = 10000)
//  @Transactional
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
//    log.info("Se encontraron {} facturas pendientes", pendientes.size());
//
//    for (FacturaBitacora bitacora : pendientes) {
//      try {
//        procesarFacturaFlow(bitacora);
//      } catch (Exception e) {
//        log.error("❌ Error procesando factura {}: {}", bitacora.getClave(), e.getMessage(), e);
//        manejarError(bitacora, e);
//      }
//    }
//
//    log.info("🏁 Job de procesamiento finalizado");
//  }
//
//  /**
//   * Flow completo de una factura: XML -> Firmar -> Enviar -> (si POST falla: reconsulta) -> Consultar -> Guardar respuesta -> PDF -> Email
//   */
//  private void procesarFacturaFlow(FacturaBitacora bitacora) throws Exception {
//    log.info("➡️ Procesando factura {} (intento #{})", bitacora.getClave(),
//        bitacora.getIntentos() + 1);
//
//    // 1) Marcar intento
//    bitacora.setIntentos(bitacora.getIntentos() + 1);
//    bitacora.setEstado(EstadoBitacora.PROCESANDO);
//    bitacoraRepository.save(bitacora);
//
//    Factura factura = facturaRepository.findById(bitacora.getFacturaId())
//        .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + bitacora.getFacturaId()));
//    final Empresa empresa = factura.getSucursal().getEmpresa();
//    final String empresaNombre = empresa.getNombreComercial();
//    final boolean produccion = empresa.getConfigHacienda().getAmbiente() == AmbienteHacienda.PRODUCCION;
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
//    boolean postOk = false;
//    try {
//      haciendaService.postRecepcion(token.getAccessToken(), produccion, req);
//      postOk = true;
//      factura.setEstado(EstadoFactura.ENVIADA);
//      facturaRepository.save(factura);
//      bitacora.setHaciendaMensaje("Enviada a MH");
//      bitacoraRepository.save(bitacora);
//    } catch (HttpClientErrorException ex) {
//      // Si POST falla, intentamos reconsultar estado por clave (puede que ya estuviera enviada)
//      log.warn("[MH] POST /recepcion falló {} {}. Intentando reconsulta por clave {}...",
//          ex.getStatusCode(), ex.getStatusText(), factura.getClave());
//      ConsultaEstadoResponse estadoTrasPostFallido = consultarEstadoConRefreshSi400(empresa, produccion, factura.getClave(), token.getAccessToken());
//
//      if (estadoTrasPostFallido != null) {
//        String ind = safeUpper(estadoTrasPostFallido.getIndEstado());
//        if ("ACEPTADO".equals(ind) || "RECHAZADO".equals(ind)) {
//          // Guardar respuesta y cerrar ciclo
//          guardarRespuestaDeHacienda(bitacora, factura, estadoTrasPostFallido, empresaNombre);
//          if ("ACEPTADO".equals(ind)) {
//            factura.setEstado(EstadoFactura.ACEPTADA);
//            bitacora.setEstado(EstadoBitacora.ACEPTADA);
//            bitacora.setHaciendaMensaje("Aceptada por MH (tras POST fallido)");
//            facturaRepository.save(factura);
//            bitacoraRepository.save(bitacora);
//
//            // Solo Factura (01) genera PDF/Email
//            if ("01".equals(factura.getTipoDocumento().getCodigo())) {
//              try {
//                log.info("[5/6] Generando PDF...");
//                byte[] pdf = pdfService.generarFacturaCarta(factura.getClave());
//                log.info("[6/6] Enviando correo...");
//                enviarEmail(bitacora, factura, pdf);
//              } catch (Exception mailEx) {
//                log.error("❌ Error PDF/Email tras POST fallido: {}", mailEx.getMessage(), mailEx);
//              }
//            } else {
//              log.info("Tipo {} aceptado (tras POST fallido): no PDF/Email.", factura.getTipoDocumento().getCodigo());
//            }
//            log.info("✅ Factura {} completada con estado {}", factura.getClave(), factura.getEstado());
//            return; // ya cerramos
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
//      // Si no hay terminal, marcamos para retry
//      manejarError(bitacora, ex);
//      return;
//    }
//
//    // 5) Si POST fue OK → hacer poll
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
//    // 6) Generar PDF + Email si ACEPTADA y es Factura (01)
//    if (factura.getEstado() == EstadoFactura.ACEPTADA && "01".equals(factura.getTipoDocumento().getCodigo())) {
//      log.info("[5/6] Generando PDF...");
//      byte[] pdf = pdfService.generarFacturaCarta(factura.getClave());
//
//      log.info("[6/6] Enviando correo...");
//      enviarEmail(bitacora, factura, pdf);
//
//      bitacora.setEstado(EstadoBitacora.ACEPTADA); // redundante, por si acaso
//      bitacoraRepository.save(bitacora);
//    }
//    log.info("✅ Factura {} completada con estado {}", factura.getClave(), factura.getEstado());
//  }
//
//  /**
//   * Intenta GET /recepcion/{clave}. Si recibe 400, renueva token y reintenta 1 vez.
//   * Devuelve null si no se pudo obtener respuesta o no hay indEstado.
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
//   * Guarda la respuesta XML (si vino en base64 o bytes) en tu storage y actualiza bitácora
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
//}