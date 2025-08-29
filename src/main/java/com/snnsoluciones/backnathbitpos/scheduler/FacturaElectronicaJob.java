package com.snnsoluciones.backnathbitpos.scheduler;

import com.snnsoluciones.backnathbitpos.dto.email.EmailFacturaDto;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.entity.FacturaBitacora;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.enums.facturacion.TipoArchivoFactura;
import com.snnsoluciones.backnathbitpos.enums.mh.AmbienteHacienda;
import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.HaciendaClient;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.ConsultaEstadoResponse;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.HaciendaAuthParams;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.HaciendaTokenResponse;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.IdentificacionDTO;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.RecepcionRequest;
import com.snnsoluciones.backnathbitpos.repository.FacturaBitacoraRepository;
import com.snnsoluciones.backnathbitpos.repository.FacturaRepository;
import com.snnsoluciones.backnathbitpos.service.EmailService;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import com.snnsoluciones.backnathbitpos.service.pdf.FacturaPdfService;
import com.snnsoluciones.backnathbitpos.util.ByteArrayMultipartFile;
import com.snnsoluciones.backnathbitpos.util.FacturaFirmaService;
import com.snnsoluciones.backnathbitpos.util.S3PathBuilder;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Job para procesamiento asíncrono de facturas electrónicas Se ejecuta cada minuto y procesa las
 * facturas pendientes
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FacturaElectronicaJob {

  private final FacturaBitacoraRepository bitacoraRepository;
  private final FacturaRepository facturaRepository;
  private final FacturaXMLGeneratorService xmlGeneratorService;
  private final FacturaFirmaService firmaService;
  private final HaciendaClient haciendaService;
  private final FacturaPdfService pdfService;
  private final EmailService emailService;
  private final StorageService storageService;
  private final S3PathBuilder s3PathBuilder;

  // Configuración
  private static final int MAX_FACTURAS_POR_CICLO = 10;
  private static final int MAX_INTENTOS = 3;


  /**
   * Procesa facturas pendientes cada 60 segundos
   * <p>
   * fixedDelay asegura que espera 60 segundos DESPUÉS de que termine la ejecución anterior (evita
   * overlapping)
   */
  @Scheduled(fixedDelay = 60000, initialDelay = 10000)
  @Transactional
  public void procesarFacturasPendientes() {
    log.info("⏳ Iniciando job de procesamiento de facturas electrónicas...");

    List<FacturaBitacora> pendientes = bitacoraRepository.findFacturasPendientesProcesar(
        LocalDateTime.now(),
        PageRequest.of(0, MAX_FACTURAS_POR_CICLO)
    );

    if (pendientes.isEmpty()) {
      log.debug("No hay facturas pendientes para procesar");
      return;
    }

    log.info("Se encontraron {} facturas pendientes", pendientes.size());

    for (FacturaBitacora bitacora : pendientes) {
      try {
        procesarFacturaFlow(bitacora);
      } catch (Exception e) {
        log.error("❌ Error procesando factura {}: {}", bitacora.getClave(), e.getMessage(), e);
        manejarError(bitacora, e);
      }
    }

    log.info("🏁 Job de procesamiento finalizado");
  }

  /**
   * Flow completo de una factura: XML -> Firmar -> Enviar -> Consultar -> Guardar respuesta -> PDF -> Email
   */
  private void procesarFacturaFlow(FacturaBitacora bitacora) throws Exception {
    log.info("➡️ Procesando factura {} (intento #{})", bitacora.getClave(), bitacora.getIntentos() + 1);

    // 1) Marcar intento
    bitacora.setIntentos(bitacora.getIntentos() + 1);
    bitacora.setEstado(EstadoBitacora.PROCESANDO);
    bitacoraRepository.save(bitacora);

    Factura factura = facturaRepository.findById(bitacora.getFacturaId())
        .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + bitacora.getFacturaId()));
    final Empresa empresa = factura.getSucursal().getEmpresa();
    final String empresaNombre = empresa.getNombreRazonSocial();

    // 2) Generar XML
    log.info("[1/6] Generando XML...");
    String xml = xmlGeneratorService.generarXML(factura.getId());
    String xmlPath = s3PathBuilder.buildXmlPath(factura, TipoArchivoFactura.XML_UNSIGNED,
        empresa.getNombreComercial());
    storageService.uploadFile(
        createMultipartFile(xml.getBytes(StandardCharsets.UTF_8), "factura_" + factura.getClave() + ".xml", "text/xml"),
        xmlPath
    );
    bitacora.setXmlPath(xmlPath);
    bitacoraRepository.save(bitacora);

    // 3) Firmar XML
    log.info("[2/6] Firmando XML...");
    byte[] xmlFirmado = firmaService.firmarXML(xmlPath, empresa.getId());
    String xmlFirmadoPath = s3PathBuilder.buildXmlPath(factura, TipoArchivoFactura.XML_SIGNED,
        empresa.getNombreComercial());
    storageService.uploadFile(
        createMultipartFile(xmlFirmado, "factura_" + factura.getClave() + "_firmado.xml", "text/xml"),
        xmlFirmadoPath
    );
    bitacora.setXmlFirmadoPath(xmlFirmadoPath);
    bitacoraRepository.save(bitacora);

    // 4) Enviar a Hacienda
    log.info("[3/6] Enviando a Hacienda...");
    HaciendaTokenResponse token = haciendaService.getToken(
        HaciendaAuthParams.builder()
            .empresaId(empresa.getId())
            .username(empresa.getConfigHacienda().getUsuarioHacienda())
            .password(empresa.getConfigHacienda().getClaveHacienda())
            .clientId(empresa.getConfigHacienda().getAmbiente() == AmbienteHacienda.PRODUCCION ? "api-prod" : "api-test")
            .sandbox(empresa.getConfigHacienda().getAmbiente() != AmbienteHacienda.PRODUCCION)
            .build()
    );

    // receptor opcional
    IdentificacionDTO receptor = null;
    if (!"04".equals(factura.getTipoDocumento().getCodigo()) &&
        factura.getCliente() != null && factura.getCliente().getNumeroIdentificacion() != null) {
      receptor = IdentificacionDTO.builder()
          .tipoIdentificacion(factura.getCliente().getTipoIdentificacion().getCodigo())
          .numeroIdentificacion(factura.getCliente().getNumeroIdentificacion())
          .build();
    }

    RecepcionRequest req = RecepcionRequest.builder()
        .clave(factura.getClave())
        .fecha(formatearFechaHacienda(factura.getFechaEmision()))
        .emisor(IdentificacionDTO.builder()
            .tipoIdentificacion(empresa.getTipoIdentificacion().getCodigo())
            .numeroIdentificacion(empresa.getIdentificacion())
            .build())
        .receptor(receptor)
        .comprobanteXml(Base64.getEncoder().encodeToString(xmlFirmado))
        .build();

    haciendaService.postRecepcion(token.getAccessToken(),
        empresa.getConfigHacienda().getAmbiente() == AmbienteHacienda.PRODUCCION, req);

    factura.setEstado(EstadoFactura.ENVIADA);
    facturaRepository.save(factura);
    bitacora.setHaciendaMensaje("Enviada a MH");
    bitacoraRepository.save(bitacora);

    // 5) Consultar estado
    log.info("[4/6] Consultando estado en MH...");
    ConsultaEstadoResponse estado = pollEstadoHacienda(token.getAccessToken(),
        empresa.getConfigHacienda().getAmbiente() == AmbienteHacienda.PRODUCCION,
        factura.getClave(), 6, 10);

    if (estado != null) {
      guardarRespuestaDeHacienda(bitacora, factura, estado, empresaNombre);

      if ("ACEPTADO".equalsIgnoreCase(estado.getIndEstado())) {
        factura.setEstado(EstadoFactura.ACEPTADA);
        bitacora.setHaciendaMensaje("Aceptada por MH");
      } else if ("RECHAZADO".equalsIgnoreCase(estado.getIndEstado()) || "ERROR".equalsIgnoreCase(estado.getIndEstado())) {
        factura.setEstado(EstadoFactura.RECHAZADA);
        bitacora.setHaciendaMensaje(estado.getDetalleMensaje() != null
            ? estado.getDetalleMensaje()
            : "Rechazada/Error en MH");
      } else {
        factura.setEstado(EstadoFactura.ENVIADA); // sigue en RECIBIDO/PROCESANDO
        bitacora.setHaciendaMensaje("Pendiente en MH: " + estado.getIndEstado());
      }

      facturaRepository.save(factura);
      bitacoraRepository.save(bitacora);
    }

    // 6) Generar PDF + Email si aceptada
    if (factura.getEstado() == EstadoFactura.ACEPTADA) {
      log.info("[5/6] Generando PDF...");
      byte[] pdf = pdfService.generarFacturaCarta(factura.getClave());

      log.info("[6/6] Enviando correo...");
      enviarEmail(bitacora, factura, pdf);
    }

    log.info("✅ Factura {} completada con estado {}", factura.getClave(), factura.getEstado());
  }

  /**
   * Job de limpieza - ejecuta cada día a las 2 AM Limpia procesos stuck o registros antiguos
   */
  @Scheduled(cron = "0 0 2 * * *")
  public void limpiezaDiaria() {
    log.info("Ejecutando limpieza diaria de bitácora...");
  }

  /**
   * Job de monitoreo - cada 5 minutos verifica salud del sistema Útil para alertas tempranas
   */
  @Scheduled(fixedRate = 300000)
  public void monitorearSalud() {
    log.info("Ejecutando monitoreo de salud del sistema...");
  }


  /**
   * Poll corto: consulta /recepcion/{clave} hasta estado terminal o agotar intentos
   */
  private ConsultaEstadoResponse pollEstadoHacienda(
      String accessToken, boolean produccion, String clave, int maxIntentos, int sleepSegs
  ) {
    for (int i = 0; i < maxIntentos; i++) {
      ConsultaEstadoResponse r = haciendaService.getEstado(accessToken, !produccion ? true : false,
          clave);
      // uniformar a mayúsculas
      String e = r != null ? safeUpper(r.getIndEstado()) : null;

      if ("ACEPTADO".equals(e) || "RECHAZADO".equals(e)) {
        return r;
      }

      // sigue procesando
      try {
        Thread.sleep(sleepSegs * 1000L);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        return null;
      }
    }
    return null;
  }

  /**
   * Guarda la respuesta XML (si vino en base64 o bytes) en tu storage y actualiza bitácora
   */
  private void guardarRespuestaDeHacienda(FacturaBitacora bitacora, Factura factura,
      ConsultaEstadoResponse estado, String empresaNombre) {
    try {
      byte[] respuestaBytes = null;

      // Asumo tu DTO trae la respuesta como Base64 (ajusta si ya la traes en bytes)
      // nombres típicos: getXmlRespuesta(), getRespuestaXml(), etc.
      if (estado.getRespuestaXmlBase64() != null && !estado.getRespuestaXmlBase64().isBlank()) {
        try {
          respuestaBytes = Base64.getDecoder().decode(estado.getRespuestaXmlBase64());
        } catch (IllegalArgumentException badB64) {
          // por si ya viene como xml plano
          respuestaBytes = estado.getRespuestaXmlBase64().getBytes(StandardCharsets.UTF_8);
        }
      }

      if (respuestaBytes != null) {
        String respuestaPath = s3PathBuilder.buildXmlPath(factura,
            TipoArchivoFactura.XML_RESPUESTA, empresaNombre);
        MultipartFile respuestaFile = createMultipartFile(
            respuestaBytes,
            "factura_" + factura.getClave() + "_respuesta.xml",
            "application/xml"
        );
        storageService.uploadFile(respuestaFile, respuestaPath);
        bitacora.setXmlRespuestaPath(respuestaPath);
      }
    } catch (Exception ex) {
      log.warn("No se pudo guardar el XML de respuesta para clave {}: {}", factura.getClave(),
          ex.getMessage());
    }
  }

  /**
   * Utilidad: mayúsculas seguro con nulos
   */
  private static String safeUpper(String s) {
    return s == null ? null : s.trim().toUpperCase();
  }

  /**
   * PASO 5: Enviar Email
   */

  private void enviarEmail(FacturaBitacora bitacora, Factura factura, byte[] pdfBytes) {
    try {
      // Solo enviar si el cliente tiene email
      if (factura.getCliente() != null &&
          factura.getCliente().getEmails() != null &&
          !factura.getCliente().getEmails().isEmpty()) {

        // 1) Descargar bytes de XML firmado y respuesta desde tu storage (S3, etc.)
        byte[] xmlFirmadoBytes = null;
        if (bitacora.getXmlFirmadoPath() != null) {
          xmlFirmadoBytes = storageService.downloadFileAsBytes(bitacora.getXmlFirmadoPath());
        }

        byte[] respuestaHaciendaBytes = null;
        if (bitacora.getXmlRespuestaPath() != null) {
          respuestaHaciendaBytes = storageService.downloadFileAsBytes(bitacora.getXmlRespuestaPath());
        }

        // 2) Armar el DTO que espera el EmailService
        String emailDestino = factura.getCliente().getEmails(); // o une varios si aplica
        EmailFacturaDto dto = EmailFacturaDto.builder()
            // Identificadores
            .facturaId(factura.getId())
            .clave(factura.getClave())
            .consecutivo(factura.getConsecutivo())
            // Destinatario
            .emailDestino(emailDestino)
            // Datos para asunto/cuerpo (ajusta getters según tus entidades)
            .tipoDocumento(factura.getTipoDocumento().getDescripcion())
            .nombreComercial(factura.getSucursal().getEmpresa().getNombreComercial())
            .razonSocial(factura.getSucursal().getEmpresa().getNombreRazonSocial())
            .cedulaJuridica(factura.getSucursal().getEmpresa().getIdentificacion())
            .fechaEmision(factura.getFechaEmision().toString()) // si necesitas formato, dale formato
            .logoUrl(factura.getSucursal().getEmpresa().getLogoUrl())
            // Adjuntos (bytes)
            .pdfBytes(pdfBytes)
            .xmlFirmadoBytes(xmlFirmadoBytes)
            .respuestaHaciendaBytes(respuestaHaciendaBytes)
            .build();

        // 3) Enviar
        emailService.enviarFacturaElectronica(dto);
        log.info("📧 Email enviado a: {}", emailDestino);
      }

    } catch (Exception e) {
      // Email no es crítico, solo loguear error
      log.error("❌ Error enviando email para factura {}: {}", factura.getClave(), e.getMessage());
    }
  }

  /**
   * Maneja errores durante el procesamiento
   */
  private void manejarError(FacturaBitacora bitacora, Exception e) {
    log.error("Error procesando factura {}: {}", bitacora.getClave(), e.getMessage());

    // Actualizar bitácora con error
    bitacora.setUltimoError(e.getMessage());

    if (bitacora.getIntentos() >= MAX_INTENTOS) {
      // Marcar como error permanente
      bitacora.setEstado(EstadoBitacora.ERROR);
      log.error("Factura {} marcada como ERROR después de {} intentos",
          bitacora.getClave(), MAX_INTENTOS);
    } else {
      // Programar reintento
      bitacora.setEstado(EstadoBitacora.PENDIENTE);
      bitacora.setProximoIntento(calcularProximoIntento(bitacora.getIntentos()));
      log.warn("Factura {} será reintentada en {}",
          bitacora.getClave(), bitacora.getProximoIntento());
    }

    bitacoraRepository.save(bitacora);
  }

  /**
   * Calcula el próximo intento con backoff exponencial
   */
  private LocalDateTime calcularProximoIntento(int intentos) {
    // Backoff: 5 min, 15 min, 30 min
    int minutos = switch (intentos) {
      case 1 -> 5;
      case 2 -> 15;
      default -> 30;
    };
    return LocalDateTime.now().plusMinutes(minutos);
  }

  /**
   * Helper para crear MultipartFile desde bytes
   */
  private MultipartFile createMultipartFile(byte[] content, String fileName, String contentType) {
    return new ByteArrayMultipartFile(content, "file", fileName, contentType);
  }

  private static final DateTimeFormatter HACIENDA_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

  private String formatearFechaHacienda(String fechaEmision) {
    // Parsear el String (ISO-8601 flexible)
    ZonedDateTime zdt = ZonedDateTime.parse(fechaEmision);
    return zdt.format(HACIENDA_FMT); // salida: 2025-08-28T23:50:00-0600
  }
}