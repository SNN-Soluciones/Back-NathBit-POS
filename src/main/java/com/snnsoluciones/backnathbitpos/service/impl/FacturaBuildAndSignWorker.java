package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.entity.FacturaDocumento;
import com.snnsoluciones.backnathbitpos.entity.FacturaDocumentoHacienda;
import com.snnsoluciones.backnathbitpos.entity.FacturaJob;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.enums.facturacion.PasoFacturacion;
import com.snnsoluciones.backnathbitpos.enums.facturacion.TipoArchivoFactura;
import com.snnsoluciones.backnathbitpos.enums.mh.AmbienteHacienda;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.HaciendaClient;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.TokenService;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.HaciendaAuthParams;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.IdentificacionDTO;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.RecepcionRequest;
import com.snnsoluciones.backnathbitpos.repository.FacturaDocumentoHaciendaRepository;
import com.snnsoluciones.backnathbitpos.repository.FacturaDocumentoRepository;
import com.snnsoluciones.backnathbitpos.repository.FacturaRepository;
import com.snnsoluciones.backnathbitpos.scheduler.FacturaXMLGeneratorService;
import com.snnsoluciones.backnathbitpos.service.FacturaAsyncProcessor;
import com.snnsoluciones.backnathbitpos.service.FacturaJobService;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import com.snnsoluciones.backnathbitpos.util.FacturaFirmaService;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacturaBuildAndSignWorker implements FacturaAsyncProcessor {

  private final FacturaJobService jobService;
  private final FacturaRepository facturaRepository;
  private final FacturaDocumentoRepository documentoRepository;
  private final FacturaDocumentoHaciendaRepository docHaciendaRepository;
  private final HaciendaClient haciendaClient;
  private final TokenService tokenService;
  private final StorageService storageService;
  private final FacturaXMLGeneratorService xmlGenerator;
  private final FacturaFirmaService firmaService;

  @Value("${factura.processor.enabled:true}")
  private boolean processorEnabled;

  @Value("${factura.processor.batch-size:10}")
  private int batchSize;

  @Value("${app.ambiente-hacienda:SANDBOX}") // SANDBOX | PRODUCCION
  private String ambienteHda;

  // zona horaria de Costa Rica
  private static final ZoneId ZONE_CR = ZoneId.of("America/Costa_Rica");

  // formatter que genera 2025-08-25T14:35:00-0600 (nota: Z sin los :)
  private static final DateTimeFormatter ISO_OFFSET_NO_COLON =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

  // ============================
  // SÓLO PASOS 1 y 2 (Build + Sign)
  // ============================
  @Override
  @Scheduled(fixedDelayString = "${factura.processor.delay.buildsign:30000}")
  public void procesarJobsPendientes() {
    if (!processorEnabled) {
      log.debug("[Build&Sign] Procesador deshabilitado");
      return;
    }

    final Set<PasoFacturacion> pasos = EnumSet.of(
        PasoFacturacion.GENERAR_XML,
        PasoFacturacion.FIRMAR_DOCUMENTO
    );

    log.info("========== Build&Sign :: INICIO ciclo ==========");
    List<FacturaJob> jobs = jobService.obtenerJobsPendientesPorPasos(pasos, batchSize);
    log.info("[Build&Sign] Jobs candidatos: {}", jobs.size());

    for (FacturaJob job : jobs) {
      try {
        procesarFactura(job);
      } catch (Exception e) {
        log.error("[Build&Sign] Error procesando job {} (clave {}): {}",
            job.getId(), job.getClave(), e.getMessage(), e);
        jobService.marcarError(job.getId(), e.getMessage());
      }
    }
    log.info("========== Build&Sign :: FIN  ciclo ==========\n");
  }

  @Override
  @Transactional
  public void procesarFactura(FacturaJob job) {
    log.info("[Build&Sign] Procesando job {} :: clave={} :: paso={}",
        job.getId(), job.getClave(), job.getPasoActual());

    // Carga robusta de factura
    Factura factura = facturaRepository.findByIdWithRelaciones(job.getFacturaId())
        .orElseThrow(() -> new IllegalStateException(
            "[Build&Sign] Factura no encontrada id=" + job.getFacturaId()));

    // Siempre dejamos trazado el estado de la factura
    if (factura.getEstado() != EstadoFactura.PROCESANDO) {
      factura.setEstado(EstadoFactura.PROCESANDO);
      facturaRepository.save(factura);
      log.debug("[Build&Sign] Factura {} marcada como PROCESANDO", factura.getId());
    }

    switch (job.getPasoActual()) {
      case GENERAR_XML -> ejecutarGenerarXml(job, factura);
      case FIRMAR_DOCUMENTO -> ejecutarFirmarXml(job, factura);
      default -> log.debug("[Build&Sign] Job {} con paso {} no es atendido por este worker",
          job.getId(), job.getPasoActual());
    }
  }

  // ============================
  // Paso 1: Generar XML (unsigned) y subirlo a S3
  // ============================
  private void ejecutarGenerarXml(FacturaJob job, Factura factura) {
    log.info("[Build&Sign][{}] Paso 1/2 :: GENERAR_XML", job.getClave());

    // 1) Generar contenido XML (usa tu generador 4.4)
    String xml = xmlGenerator.generarXML(factura.getId());
    byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
    log.info("[Build&Sign][{}] XML generado ({} chars, {} bytes aprox.)",
        job.getClave(), xml.length(), bytes.length);

    String rootFolder = "NathBit-POS";

    String empresaNombre = factura.getSucursal().getEmpresa().getNombreComercial()
        .toUpperCase()
        .replaceAll("\\s+", "_");

    String tipoDocumento = factura.getTipoDocumento().name();

// Mes en español
    String mes = LocalDate.now().getMonth()
        .getDisplayName(TextStyle.FULL, new Locale("es", "ES"))
        .toUpperCase();

// Construcción de la key en formato carpeta
    String s3Key = String.format(
        "%s/%s/%s/%s/%s-%s.xml",
        rootFolder,
        empresaNombre,
        tipoDocumento,
        mes,
        factura.getClave(),
        TipoArchivoFactura.XML_UNSIGNED
    );

    storageService.uploadFile(new ByteArrayInputStream(bytes), s3Key, "application/xml",
        bytes.length);

    log.info("[Build&Sign][{}] XML_UNSIGNED subido a S3 -> {}", job.getClave(), s3Key);

    // 3) Registrar/actualizar FacturaDocumento (idempotente)
    documentoRepository.findByClaveAndTipoArchivo(factura.getClave(),
            TipoArchivoFactura.XML_UNSIGNED)
        .ifPresentOrElse(doc -> {
          // ya existe → actualiza metadatos si quieres
          doc.setS3Key(s3Key);
          doc.setTamanio((long) bytes.length);
          documentoRepository.save(doc);
          log.debug("[Build&Sign][{}] XML_UNSIGNED idempotente actualizado en BD", job.getClave());
        }, () -> {
          FacturaDocumento doc = new FacturaDocumento();
          doc.setFacturaId(factura.getId());
          doc.setClave(factura.getClave());
          doc.setTipoArchivo(TipoArchivoFactura.XML_UNSIGNED);
          doc.setS3Key(s3Key);
          doc.setTamanio((long) bytes.length);
          doc.setCreatedAt(LocalDateTime.now());
          documentoRepository.save(doc);
          log.info("[Build&Sign][{}] XML_UNSIGNED registrado en BD (id={})", job.getClave(),
              doc.getId());
        });

    // 4) Avanzar el job
    jobService.avanzarPaso(job.getId(), PasoFacturacion.FIRMAR_DOCUMENTO);
    log.info("[Build&Sign][{}] Paso cambiado a FIRMAR_DOCUMENTO", job.getClave());
  }

  // ============================
  // Paso 2: Firmar XML y subirlo a S3 + sincronizar Doc Hacienda
  // ============================
  private void ejecutarFirmarXml(FacturaJob job, Factura factura) {
    log.info("[Build&Sign][{}] Paso 2/2 :: FIRMAR_DOCUMENTO", job.getClave());

    // 1) Resolver el unsigned desde BD
    List<FacturaDocumento> dbg = documentoRepository.findByClave(factura.getClave());
    log.info("[Build&Sign][{}] Docs por clave {} -> {}", job.getClave(), factura.getClave(),
        dbg.size());

// 1:1 clave+tipo (tu método nuevo)
    String unsignedKey = documentoRepository.findOneByClaveAndTipoArchivo(
            factura.getClave(), TipoArchivoFactura.XML_UNSIGNED)
        .map(FacturaDocumento::getS3Key)
        .orElseThrow(() -> new IllegalStateException(
            "[Build&Sign] No existe XML_UNSIGNED para clave " + factura.getClave()));

    log.debug("[Build&Sign][{}] Unsigned key => {}", job.getClave(), unsignedKey);

    // 2) Firmar y generar bytes del firmado
    byte[] signedBytes;
    try {
      signedBytes = firmaService.firmarXML(
          unsignedKey,
          factura.getSucursal().getEmpresa().getId());

    } catch (Exception e) {
      throw new IllegalStateException("[Build&Sign] Error firmando XML: " + e.getMessage(), e);
    }

    // 3) Construir misma ruta que el unsigned pero con sufijo SIGNED
    String rootFolder = "NathBit-POS";
    String empresaNombre = factura.getSucursal().getEmpresa().getNombreComercial()
        .toUpperCase()
        .replaceAll("\\s+", "_");
    String tipoDocumento = factura.getTipoDocumento().name();
    String mes = LocalDate.now().getMonth()
        .getDisplayName(TextStyle.FULL, new Locale("es", "ES"))
        .toUpperCase();

    String signedKey = String.format(
        "%s/%s/%s/%s/%s-%s.xml",
        rootFolder,
        empresaNombre,
        tipoDocumento,
        mes,
        factura.getClave(),
        TipoArchivoFactura.XML_SIGNED
    );

    // 4) Subir firmado a S3
    storageService.uploadFile(new ByteArrayInputStream(signedBytes), signedKey, "application/xml",
        signedBytes.length);
    log.info("[Build&Sign][{}] XML_SIGNED generado y subido -> {}", job.getClave(), signedKey);

    // 5) Registrar/actualizar FacturaDocumento (XML_SIGNED)
    documentoRepository.findByClaveAndTipoArchivo(factura.getClave(), TipoArchivoFactura.XML_SIGNED)
        .ifPresentOrElse(doc -> {
          doc.setS3Key(signedKey);
          doc.setTamanio((long) signedBytes.length);
          documentoRepository.save(doc);
          log.debug("[Build&Sign][{}] XML_SIGNED idempotente actualizado en BD", job.getClave());
        }, () -> {
          FacturaDocumento doc = new FacturaDocumento();
          doc.setFacturaId(factura.getId());
          doc.setClave(factura.getClave());
          doc.setTipoArchivo(TipoArchivoFactura.XML_SIGNED);
          doc.setS3Key(signedKey);
          doc.setTamanio((long) signedBytes.length);
          doc.setCreatedAt(LocalDateTime.now());
          documentoRepository.save(doc);
          log.info("[Build&Sign][{}] XML_SIGNED registrado en BD (id={})", job.getClave(),
              doc.getId());
        });

    // 6) Sincronizar FacturaDocumentoHacienda
    AmbienteHacienda ambiente = factura.getSucursal()
        .getEmpresa()
        .getConfigHacienda().getAmbiente();

    FacturaDocumentoHacienda hda = docHaciendaRepository.findByClave(job.getClave())
        .orElseGet(() -> {
          FacturaDocumentoHacienda nuevo = new FacturaDocumentoHacienda();
          nuevo.setFacturaId(factura.getId());
          nuevo.setClave(job.getClave());
          nuevo.setAmbiente(ambiente);
          return nuevo;
        });

    hda.setS3KeyXmlFirmado(signedKey);
    hda.setFechaEstado(LocalDateTime.now());
    hda.setUpdatedAt(LocalDateTime.now());
    if (hda.getCreatedAt() == null) {
      hda.setCreatedAt(LocalDateTime.now());
    }
    docHaciendaRepository.save(hda);
    log.info("[Build&Sign][{}] DocHacienda sincronizado (ambiente={}, s3KeyXmlFirmado set)",
        job.getClave(), hda.getAmbiente());

    // 7) Avanzar el job al siguiente paso
    jobService.avanzarPaso(job.getId(), PasoFacturacion.ENVIAR_HACIENDA);
    log.info("[Build&Sign][{}] Paso cambiado a ENVIAR_HACIENDA", job.getClave());
  }

  private void ejecutarEnviarHacienda(FacturaJob job, Factura factura) {
    log.info("[Send&Status][{}] ENVIAR_HACIENDA", job.getClave());

    // 1) Resolver el XML firmado desde BD y S3
    String signedKey = documentoRepository.findOneByClaveAndTipoArchivo(
            factura.getClave(), TipoArchivoFactura.XML_SIGNED)
        .map(FacturaDocumento::getS3Key)
        .orElseThrow(() -> new IllegalStateException("No existe XML_SIGNED para clave " + factura.getClave()));

    byte[] signedBytes = storageService.downloadFileAsBytes(signedKey);
    String xmlBase64 = Base64.getEncoder().encodeToString(signedBytes);

    // 2) Armar auth params desde la EMPRESA
    Empresa empresa = factura.getSucursal().getEmpresa();
    AmbienteHacienda ambiente = empresa.getConfigHacienda().getAmbiente();
    boolean sandbox = (ambiente == AmbienteHacienda.SANDBOX);
    String clientId = sandbox ? "api-stag" : "api-prod";

    HaciendaAuthParams auth = HaciendaAuthParams.builder()
        .empresaId(empresa.getId())
        .sandbox(sandbox)
        .clientId(clientId)
        .username(empresa.getConfigHacienda().getUsuarioHacienda())
        .password(empresa.getConfigHacienda().getClaveHacienda())
        .build();

    // 3) Token
    String token = tokenService.getValidToken(auth);

    // 4) Construir payload
    String fecha = LocalDateTime.now(ZONE_CR).format(ISO_OFFSET_NO_COLON); // e.g. 2025-08-25T14:35:00-0600

    IdentificacionDTO emisor = IdentificacionDTO.builder()
        .tipoIdentificacion(empresa.getTipoIdentificacion().getCodigo())
        .numeroIdentificacion(empresa.getIdentificacion().replaceAll("-", ""))
        .build();

    IdentificacionDTO receptor = null;
    if (factura.getCliente().getTipoIdentificacion() != null && factura.getCliente().getNumeroIdentificacion() != null) {
      receptor = IdentificacionDTO.builder()
          .tipoIdentificacion(factura.getCliente().getTipoIdentificacion().getCodigo())
          .numeroIdentificacion(factura.getCliente().getNumeroIdentificacion().replaceAll("-", ""))
          .build();
    }

//    String callbackUrl = (empresa.getConfigHacienda().getCallbackUrl() != null && !empresa.getConfigHacienda().getCallbackUrl().isBlank())
//        ? empresa.getConfigHacienda().getCallbackUrl()
//        : (defaultCallbackUrl == null || defaultCallbackUrl.isBlank() ? null : defaultCallbackUrl);

    RecepcionRequest payload = RecepcionRequest.builder()
        .clave(factura.getClave())
        .fecha(fecha)
        .emisor(emisor)
        .receptor(receptor)
        .comprobanteXml(xmlBase64)
        .callbackUrl("")
        .build();

    // 5) POST /recepcion
    String location = haciendaClient.postRecepcion(token, sandbox, payload);
    log.info("[Send&Status][{}] Enviado a Hacienda (location={})", job.getClave(), location);

    // 6) Actualizar DocHacienda con firmado si aún no estaba
    FacturaDocumentoHacienda hda = docHaciendaRepository.findByClave(job.getClave())
        .orElseGet(() -> {
          FacturaDocumentoHacienda nuevo = new FacturaDocumentoHacienda();
          nuevo.setFacturaId(factura.getId());
          nuevo.setClave(job.getClave());
          nuevo.setAmbiente(ambiente);
          return nuevo;
        });
    hda.setS3KeyXmlFirmado(signedKey);
    hda.setFechaEstado(LocalDateTime.now());
    if (hda.getCreatedAt() == null) hda.setCreatedAt(LocalDateTime.now());
    hda.setUpdatedAt(LocalDateTime.now());
    docHaciendaRepository.save(hda);

    // 7) Avanzar a CONSULTAR_ESTADO
    jobService.avanzarPaso(job.getId(), PasoFacturacion.PROCESAR_RESPUESTA);
    log.info("[Send&Status][{}] Paso cambiado a CONSULTAR_ESTADO", job.getClave());
  }

  // ====== Métodos de la interfaz que este worker no usa (no-op) ======
  @Override
  public String generarXML(Long facturaId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String firmarXML(String xmlPath) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String enviarHacienda(String xmlFirmadoPath, String clave) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String generarPDF(Long facturaId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void enviarEmail(Long facturaId, String pdfPath, String xmlPath) {
    throw new UnsupportedOperationException();
  }
}