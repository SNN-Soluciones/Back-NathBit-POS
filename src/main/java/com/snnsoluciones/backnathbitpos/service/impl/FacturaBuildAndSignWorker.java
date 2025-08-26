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
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.ConsultaEstadoResponse;
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
import com.snnsoluciones.backnathbitpos.util.S3PathBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
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
  private final S3PathBuilder s3PathBuilder;

  @Value("${factura.processor.enabled:true}")
  private boolean processorEnabled;

  @Value("${factura.processor.batch-size:10}")
  private int batchSize;

  @Value("${app.ambiente-hacienda:SANDBOX}")
  private String ambienteHda;

  // zona horaria de Costa Rica
  private static final ZoneId ZONE_CR = ZoneId.of("America/Costa_Rica");

  // formatter que genera 2025-08-25T14:35:00-0600 (nota: Z sin los :)
  private static final DateTimeFormatter ISO_OFFSET_NO_COLON =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

  // ============================
  // PROCESADOR PRINCIPAL - TODOS LOS PASOS
  // ============================
  @Override
  @Scheduled(fixedDelayString = "${factura.processor.delay.buildsign:30000}")
  public void procesarJobsPendientes() {
    if (!processorEnabled) {
      log.debug("[MainProcessor] Procesador deshabilitado");
      return;
    }

    log.info("========== MainProcessor :: INICIO ciclo ==========");

    // Buscar jobs con facturas NOT IN (ACEPTADA, RECHAZADA, ANULADA, ERROR)
    List<FacturaJob> jobs = jobService.obtenerJobsPorEstadosExcluidos(
        Arrays.asList(
            EstadoFactura.ACEPTADA,
            EstadoFactura.RECHAZADA,
            EstadoFactura.ANULADA,
            EstadoFactura.ERROR
        ),
        batchSize
    );

    log.info("[MainProcessor] Jobs candidatos: {}", jobs.size());

    for (FacturaJob job : jobs) {
      try {
        procesarFactura(job);
      } catch (Exception e) {
        log.error("[MainProcessor] Error procesando job {} (clave {}): {}",
            job.getId(), job.getClave(), e.getMessage(), e);
        jobService.marcarError(job.getId(), e.getMessage());
      }
    }
    log.info("========== MainProcessor :: FIN  ciclo ==========\n");
  }

  @Override
  @Transactional
  public void procesarFactura(FacturaJob job) throws IOException {
    log.info("[MainProcessor] Procesando job {} :: clave={} :: paso={}",
        job.getId(), job.getClave(), job.getPasoActual());

    // Carga robusta de factura
    Factura factura = facturaRepository.findByIdWithRelaciones(job.getFacturaId())
        .orElseThrow(() -> new IllegalStateException(
            "[MainProcessor] Factura no encontrada id=" + job.getFacturaId()));

    // Marcar como procesando
    jobService.marcarProcesando(job.getId());

    // Ejecutar según el paso
    switch (job.getPasoActual()) {
      case GENERAR_XML -> ejecutarGenerarXml(job, factura);
      case FIRMAR_DOCUMENTO -> ejecutarFirmarXml(job, factura);
      case ENVIAR_HACIENDA -> ejecutarEnviarHacienda(job, factura);
      case PROCESAR_RESPUESTA -> ejecutarProcesarRespuesta(job, factura);
      default -> log.warn("[MainProcessor] Paso no manejado: {}", job.getPasoActual());
    }
  }

  // ============================
  // PASO 1: GENERAR XML
  // ============================
  private void ejecutarGenerarXml(FacturaJob job, Factura factura) {
    log.info("[MainProcessor][{}] GENERAR_XML", job.getClave());

    // Actualizar estado factura
    if (factura.getEstado() != EstadoFactura.PROCESANDO) {
      factura.setEstado(EstadoFactura.PROCESANDO);
      facturaRepository.save(factura);
    }

    // 1) Generate
    String xmlContent = xmlGenerator.generarXML(factura.getId());
    log.info("[MainProcessor][{}] XML generado: {} bytes", job.getClave(), xmlContent.length());

    // 2) Save to S3

    String s3Key = s3PathBuilder.buildXmlPath(factura, S3PathBuilder.TipoArchivoS3.SIN_FIRMA);
    log.info("[MainProcessor][{}] XML sin firmar guardado en S3: {}", job.getClave(), s3Key);

    // 3) Guardar registro documento
    FacturaDocumento doc = new FacturaDocumento();
    doc.setFacturaId(factura.getId());
    doc.setClave(factura.getClave());
    doc.setTipoArchivo(TipoArchivoFactura.XML_UNSIGNED);
    doc.setS3Key(s3Key);
    doc.setTamanio(xmlContent.getBytes().length);
    doc.setCreatedAt(LocalDateTime.now());
    documentoRepository.save(doc);

    // 4) Avanzar al siguiente paso
    jobService.avanzarPaso(job.getId(), PasoFacturacion.FIRMAR_DOCUMENTO);
    log.info("[MainProcessor][{}] Paso cambiado a FIRMAR_DOCUMENTO", job.getClave());
  }

  // ============================
  // PASO 2: FIRMAR XML
  // ============================
  private void ejecutarFirmarXml(FacturaJob job, Factura factura) throws IOException {
    log.info("[MainProcessor][{}] FIRMAR_DOCUMENTO", job.getClave());

    // Actualizar estado
    factura.setEstado(EstadoFactura.FIRMADA);
    facturaRepository.save(factura);

    // 1) Buscar el XML sin firmar
    String unsignedKey = documentoRepository.findOneByClaveAndTipoArchivo(
            factura.getClave(), TipoArchivoFactura.XML_UNSIGNED)
        .map(FacturaDocumento::getS3Key)
        .orElseThrow(() -> new IllegalStateException("No existe XML_UNSIGNED para clave " + factura.getClave()));

    byte[] unsignedBytes = storageService.downloadFileAsBytes(unsignedKey);
    log.info("[MainProcessor][{}] XML sin firmar descargado: {} bytes", job.getClave(), unsignedBytes.length);

    // 2) Firmar con el certificado de la empresa
    byte[] signedBytes = firmaService.firmarXML(
        unsignedKey,
        factura.getSucursal().getEmpresa().getConfigHacienda().getId()
    );
    log.info("[MainProcessor][{}] XML firmado: {} bytes", job.getClave(), signedBytes.length);

    // 3) Guardar firmado en S3
    String signedKey = s3PathBuilder.buildXmlPath(factura, S3PathBuilder.TipoArchivoS3.FIRMADO);

    log.info("[MainProcessor][{}] XML firmado guardado en S3: {}", job.getClave(), signedKey);

    // 4) Guardar registro documento firmado
    FacturaDocumento docFirmado = new FacturaDocumento();
    docFirmado.setFacturaId(factura.getId());
    docFirmado.setClave(job.getClave());
    docFirmado.setTipoArchivo(TipoArchivoFactura.XML_SIGNED);
    docFirmado.setS3Key(signedKey);
    docFirmado.setTamanio(signedBytes.length);
    documentoRepository.save(docFirmado);

    // 5) Sincronizar DocHacienda
    AmbienteHacienda ambiente = "PRODUCCION".equals(ambienteHda) ? AmbienteHacienda.PRODUCCION : AmbienteHacienda.SANDBOX;
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

    // 6) Avanzar el job al siguiente paso
    jobService.avanzarPaso(job.getId(), PasoFacturacion.ENVIAR_HACIENDA);
    log.info("[MainProcessor][{}] Paso cambiado a ENVIAR_HACIENDA", job.getClave());
  }

  // ============================
  // PASO 3: ENVIAR A HACIENDA
  // ============================
  private static final ZoneId CR_ZONE = ZoneId.of("America/Costa_Rica");
  private static final DateTimeFormatter ISO_OFFSET_WITH_COLON = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  private void ejecutarEnviarHacienda(FacturaJob job, Factura factura) {
    log.info("[MainProcessor][{}] ENVIAR_HACIENDA", job.getClave());

    // Actualizar estado
    factura.setEstado(EstadoFactura.ENVIADA);
    facturaRepository.save(factura);

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

    // 4) Construir payload con fecha en OffsetDateTime
    String fecha = OffsetDateTime.now(CR_ZONE).format(ISO_OFFSET_WITH_COLON);

    IdentificacionDTO emisor = IdentificacionDTO.builder()
        .tipoIdentificacion(empresa.getTipoIdentificacion().getCodigo())
        .numeroIdentificacion(empresa.getIdentificacion().replaceAll("-", ""))
        .build();

    IdentificacionDTO receptor = null;
    if (factura.getCliente() != null && factura.getCliente().getTipoIdentificacion() != null) {
      receptor = IdentificacionDTO.builder()
          .tipoIdentificacion(factura.getCliente().getTipoIdentificacion().getCodigo())
          .numeroIdentificacion(factura.getCliente().getNumeroIdentificacion().replaceAll("-", ""))
          .build();
    }

    RecepcionRequest payload = RecepcionRequest.builder()
        .clave(factura.getClave())
        .fecha(fecha)
        .emisor(emisor)
        .receptor(receptor)
        .comprobanteXml(xmlBase64)
        .callbackUrl("")  // Dejar vacío por ahora
        .build();

    // 5) POST /recepcion
    try {
      String location = haciendaClient.postRecepcion(token, sandbox, payload);
      log.info("[MainProcessor][{}] Enviado a Hacienda (location={})", job.getClave(), location);

      // 6) Actualizar DocHacienda con fecha de envío
      FacturaDocumentoHacienda hda = docHaciendaRepository.findByClave(job.getClave())
          .orElseThrow(() -> new IllegalStateException("No existe DocHacienda para clave " + job.getClave()));
      hda.setFechaEnvio(LocalDateTime.now());
      hda.setUpdatedAt(LocalDateTime.now());
      docHaciendaRepository.save(hda);

      // 7) Avanzar a PROCESAR_RESPUESTA
      jobService.avanzarPaso(job.getId(), PasoFacturacion.PROCESAR_RESPUESTA);
      log.info("[MainProcessor][{}] Paso cambiado a PROCESAR_RESPUESTA", job.getClave());

    } catch (Exception e) {
      log.error("[MainProcessor][{}] Error enviando a Hacienda: {}", job.getClave(), e.getMessage());
      throw e;
    }
  }

  // ============================
  // PASO 4: PROCESAR RESPUESTA (NUEVO!)
  // ============================
  private void ejecutarProcesarRespuesta(FacturaJob job, Factura factura) {
    log.info("[MainProcessor][{}] PROCESAR_RESPUESTA", job.getClave());

    try {
      // 1) Obtener token
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

      String token = tokenService.getValidToken(auth);

      // 2) Consultar estado
      ConsultaEstadoResponse respuesta = haciendaClient.getEstado(token, sandbox, job.getClave());

      if (respuesta == null || respuesta.getIndEstado() == null) {
        // Aún no hay respuesta, reprogramar para dentro de 5 minutos
        jobService.actualizarProximaEjecucion(job.getId(), LocalDateTime.now().plusMinutes(5));
        log.info("[MainProcessor][{}] Sin respuesta aún, reprogramando para 5 min", job.getClave());
        return;
      }

      String indEstado = respuesta.getIndEstado().toLowerCase();
      log.info("[MainProcessor][{}] Estado recibido: {}", job.getClave(), indEstado);

      // 3) Guardar respuesta XML si existe
      if (respuesta.getRespuestaXmlBase64() != null) {
        guardarRespuestaXml(job, factura, respuesta.getRespuestaXmlBase64());
      }

      // 4) Procesar según estado
      if (indEstado.contains("acept")) {
        // ACEPTADA
        factura.setEstado(EstadoFactura.ACEPTADA);
        facturaRepository.save(factura);

        // Avanzar a generar PDF
        jobService.avanzarPaso(job.getId(), PasoFacturacion.GENERAR_PDF);
        log.info("[MainProcessor][{}] Factura ACEPTADA - avanzando a GENERAR_PDF", job.getClave());

      } else if (indEstado.contains("rechaz")) {
        // RECHAZADA
        factura.setEstado(EstadoFactura.RECHAZADA);
        facturaRepository.save(factura);

        // Marcar job como completado (no más procesamiento automático)
        jobService.marcarCompletado(job.getId());
        log.warn("[MainProcessor][{}] Factura RECHAZADA: {}",
            job.getClave(), respuesta.getDetalleMensaje());

      } else if (indEstado.contains("proces") || indEstado.contains("recibido")) {
        // AÚN PROCESANDO
        jobService.actualizarProximaEjecucion(job.getId(), LocalDateTime.now().plusMinutes(5));
        log.info("[MainProcessor][{}] Aún procesando en Hacienda, reprogramando", job.getClave());

      } else {
        // Estado desconocido
        log.warn("[MainProcessor][{}] Estado desconocido: {}", job.getClave(), indEstado);
        jobService.actualizarProximaEjecucion(job.getId(), LocalDateTime.now().plusMinutes(10));
      }

    } catch (Exception e) {
      log.error("[MainProcessor][{}] Error consultando estado: {}", job.getClave(), e.getMessage());

      // Si es error de autenticación, reintentar más tarde
      if (e.getMessage() != null && e.getMessage().contains("401")) {
        jobService.actualizarProximaEjecucion(job.getId(), LocalDateTime.now().plusMinutes(15));
      } else {
        throw e;
      }
    }
  }

  /**
   * Guarda la respuesta XML de Hacienda en S3
   */
  private void guardarRespuestaXml(FacturaJob job, Factura factura, String xmlBase64) {
    try {
      byte[] xmlBytes = Base64.getDecoder().decode(xmlBase64);

      String s3Key = s3PathBuilder.buildXmlPath(factura, S3PathBuilder.TipoArchivoS3.RESPUESTA);


      storageService.uploadFile(
          new ByteArrayInputStream(xmlBytes),
          s3Key,
          "application/xml",
          xmlBytes.length
      );

      // Actualizar DocHacienda
      FacturaDocumentoHacienda hda = docHaciendaRepository.findByClave(job.getClave())
          .orElseThrow(() -> new IllegalStateException("No existe DocHacienda para clave " + job.getClave()));
      hda.setS3KeyXmlRespuesta(s3Key);
      hda.setFechaEstado(LocalDateTime.now());
      hda.setUpdatedAt(LocalDateTime.now());
      docHaciendaRepository.save(hda);

      log.info("[MainProcessor][{}] Respuesta XML guardada en S3", job.getClave());

    } catch (Exception e) {
      log.error("[MainProcessor][{}] Error guardando respuesta XML: {}", job.getClave(), e.getMessage());
    }
  }

  // ====== Métodos de la interfaz que este worker ya no usa ======
  @Override
  public String generarXML(Long facturaId) {
    throw new UnsupportedOperationException("Usar ejecutarGenerarXml");
  }

  @Override
  public String firmarXML(String xmlPath) {
    throw new UnsupportedOperationException("Usar ejecutarFirmarXml");
  }

  @Override
  public String enviarHacienda(String xmlFirmadoPath, String clave) {
    throw new UnsupportedOperationException("Usar ejecutarEnviarHacienda");
  }

  @Override
  public String generarPDF(Long facturaId) {
    throw new UnsupportedOperationException("Se maneja en otro scheduler");
  }

  @Override
  public void enviarEmail(Long facturaId, String pdfPath, String xmlPath) {
    throw new UnsupportedOperationException("Se maneja en otro scheduler");
  }
}