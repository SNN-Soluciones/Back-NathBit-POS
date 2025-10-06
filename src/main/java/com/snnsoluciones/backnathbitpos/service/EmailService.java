package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.email.EmailErrorFacturaDto;
import com.snnsoluciones.backnathbitpos.dto.email.EmailFacturaDto;
import com.snnsoluciones.backnathbitpos.entity.EmailAuditLog;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.enums.EstadoEmail;
import com.snnsoluciones.backnathbitpos.enums.facturacion.TipoArchivoFactura;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import com.snnsoluciones.backnathbitpos.repository.EmailAuditLogRepository;
import com.snnsoluciones.backnathbitpos.repository.FacturaRepository;
import com.snnsoluciones.backnathbitpos.service.pdf.FacturaPdfService;
import com.snnsoluciones.backnathbitpos.util.S3PathBuilder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para envío de facturas electrónicas por email Maneja plantillas HTML, adjuntos múltiples
 * y auditoría
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender mailSender;
  private final EmailAuditLogRepository auditLogRepository;
  private final FacturaRepository facturaRepository;
  private final StorageService storageService;
  private final S3PathBuilder s3PathBuilder;
  private final FacturaPdfService facturaPdfService;

  @Value("${spring.mail.username}")
  private String emailFrom;

  @Value("${app.email.max-attachment-size:10485760}") // 10MB default
  private Long maxAttachmentSize;

  /**
   * Envía factura electrónica con todos los adjuntos
   *
   * @param dto Datos del email a enviar
   */
  @Transactional
  public void enviarFacturaElectronica(EmailFacturaDto dto) {
    log.info("Iniciando envío de factura {} a {}", dto.getClave(), dto.getEmailDestino());

    // Crear registro de auditoría
    EmailAuditLog auditLog = EmailAuditLog.builder()
        .facturaId(dto.getFacturaId())
        .clave(dto.getClave())
        .emailDestino(dto.getEmailDestino())
        .estado(EstadoEmail.PENDIENTE)
        .build();
    auditLog = auditLogRepository.save(auditLog);

    try {
      // Validar tamaño de adjuntos
      validarTamanoAdjuntos(dto, auditLog);

      // Construir y enviar email
      MimeMessage message = construirMensaje(dto, auditLog);
      mailSender.send(message);

      // Marcar como enviado
      auditLog.marcarEnviado();
      log.info("Email enviado exitosamente para factura {}", dto.getClave());

    } catch (MessagingException e) {
      // Error de mensajería - generalmente transitorio
      log.error("Error de mensajería enviando factura {}: {}", dto.getClave(), e.getMessage());
      auditLog.registrarError(e.getMessage(), "TRANSITORIO");

    } catch (IllegalArgumentException e) {
      // Error de validación - permanente
      log.error("Error de validación enviando factura {}: {}", dto.getClave(), e.getMessage());
      auditLog.registrarError(e.getMessage(), "PERMANENTE");
      auditLog.setEstado(EstadoEmail.FALLO_PERMANENTE);

    } catch (Exception e) {
      // Otros errores
      log.error("Error inesperado enviando factura {}: {}", dto.getClave(), e.getMessage(), e);
      auditLog.registrarError(e.getMessage(), determinarTipoError(e));
    }

    auditLogRepository.save(auditLog);
  }

  /**
   * Construye el mensaje MIME con HTML y adjuntos
   */
  private MimeMessage construirMensaje(EmailFacturaDto dto, EmailAuditLog auditLog)
      throws MessagingException {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

    // Configurar destinatarios
    helper.setFrom(emailFrom);
    helper.setTo(dto.getEmailDestino());
    helper.setSubject(dto.getAsunto());
    auditLog.setAsunto(dto.getAsunto());

    // Crear contenido HTML
    String htmlContent = generarHtmlFactura(dto);
    helper.setText(htmlContent, true);

    // Adjuntar archivos
    adjuntarArchivos(helper, dto, auditLog);

    return message;
  }

  /**
   * Genera el HTML del email usando los datos de la factura
   */
  private String generarHtmlFactura(EmailFacturaDto dto) {
    // Por ahora HTML hardcodeado, después podemos usar Thymeleaf
    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html>");
    html.append("<html>");
    html.append("<head>");
    html.append("<meta charset='UTF-8'>");
    html.append("<style>");
    html.append(
        "body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }");
    html.append(".container { max-width: 600px; margin: 0 auto; background-color: white; }");
    html.append(
        ".header { background-color: #f8f9fa; padding: 30px; text-align: center; border-bottom: 3px solid #007bff; }");
    html.append(".logo { max-height: 80px; margin-bottom: 15px; }");
    html.append(".content { padding: 30px; }");
    html.append(
        ".info-box { background: #e8f4fd; padding: 20px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #007bff; }");
    html.append(
        ".footer { background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #666; }");
    html.append("h2 { color: #333; margin-bottom: 10px; }");
    html.append("ul { line-height: 1.8; }");
    html.append("</style>");
    html.append("</head>");
    html.append("<body>");
    html.append("<div class='container'>");

    // Header con logo
    html.append("<div class='header'>");
    if (dto.getLogoUrl() != null) {
      html.append("<img src='").append(dto.getLogoUrl()).append("' alt='Logo' class='logo'><br>");
    }
    html.append("<h2>").append(dto.getNombreComercial()).append("</h2>");
    html.append("</div>");

    // Contenido
    html.append("<div class='content'>");
    html.append("<p>Estimado(a) cliente,</p>");
    html.append(
        "<p>Le enviamos su comprobante electrónico correspondiente a la compra realizada en nuestro establecimiento.</p>");

    // Info box
    html.append("<div class='info-box'>");
    html.append("<strong>Tipo de documento:</strong> ").append(dto.getTipoDocumento())
        .append("<br>");
    html.append("<strong>Número consecutivo:</strong> ").append(dto.getConsecutivo())
        .append("<br>");
    html.append("<strong>Clave numérica:</strong> ").append(dto.getClave()).append("<br>");
    html.append("<strong>Fecha:</strong> ").append(dto.getFechaEmision()).append("<br>");
    html.append("</div>");

    html.append("<p><strong>Documentos adjuntos:</strong></p>");
    html.append("<ul>");
    html.append("<li>Factura en formato PDF</li>");
    html.append("<li>Comprobante XML firmado digitalmente</li>");
    html.append("<li>Respuesta de validación del Ministerio de Hacienda</li>");
    html.append("</ul>");

    html.append(
        "<p>Conserve estos documentos para sus registros. El comprobante electrónico tiene la misma validez que el documento físico.</p>");
    html.append("<p>Agradecemos su preferencia.</p>");
    html.append("<p>Saludos cordiales,<br><strong>").append(dto.getNombreComercial())
        .append("</strong></p>");
    html.append("</div>");

    // Footer
    html.append("<div class='footer'>");
    html.append("<p>Este es un correo automático, por favor no responder.<br>");
    html.append(dto.getRazonSocial()).append(" - Cédula Jurídica: ").append(dto.getCedulaJuridica())
        .append("<br>");
    html.append("Comprobante electrónico autorizado mediante la resolución DGT-R-48-2016</p>");
    html.append("</div>");

    html.append("</div>");
    html.append("</body>");
    html.append("</html>");

    return html.toString();
  }

  /**
   * Adjunta los archivos al mensaje
   */
  private void adjuntarArchivos(MimeMessageHelper helper, EmailFacturaDto dto,
      EmailAuditLog auditLog) throws MessagingException {
    try {
      // 1. PDF
      if (dto.getPdfBytes() != null) {
        helper.addAttachment(
            String.format("Factura_%s.pdf", dto.getClave()),
            new ByteArrayDataSource(dto.getPdfBytes(), "application/pdf")
        );
        auditLog.setAdjuntoPdfSize((long) dto.getPdfBytes().length);
      }

      // 2. XML Firmado
      if (dto.getXmlFirmadoBytes() != null) {
        helper.addAttachment(
            String.format("FE_%s_firmado.xml", dto.getClave()),
            new ByteArrayDataSource(dto.getXmlFirmadoBytes(), "text/xml")
        );
        auditLog.setAdjuntoXmlSize((long) dto.getXmlFirmadoBytes().length);
      }

      // 3. Respuesta Hacienda
      if (dto.getRespuestaHaciendaBytes() != null) {
        helper.addAttachment(
            String.format("Respuesta_Hacienda_%s.xml", dto.getClave()),
            new ByteArrayDataSource(dto.getRespuestaHaciendaBytes(), "text/xml")
        );
        auditLog.setAdjuntoRespuestaSize((long) dto.getRespuestaHaciendaBytes().length);
      }

    } catch (Exception e) {
      throw new MessagingException("Error adjuntando archivos: " + e.getMessage(), e);
    }
  }

  /**
   * Valida que los adjuntos no excedan el tamaño máximo
   */
  private void validarTamanoAdjuntos(EmailFacturaDto dto, EmailAuditLog auditLog) {
    long totalSize = 0;

    if (dto.getPdfBytes() != null) {
      totalSize += dto.getPdfBytes().length;
    }
    if (dto.getXmlFirmadoBytes() != null) {
      totalSize += dto.getXmlFirmadoBytes().length;
    }
    if (dto.getRespuestaHaciendaBytes() != null) {
      totalSize += dto.getRespuestaHaciendaBytes().length;
    }

    if (totalSize > maxAttachmentSize) {
      throw new IllegalArgumentException(
          String.format("Tamaño total de adjuntos (%d bytes) excede el máximo permitido (%d bytes)",
              totalSize, maxAttachmentSize)
      );
    }
  }

  /**
   * Envía email de confirmación de empresa creada
   *
   * @param empresa      La empresa recién creada
   * @param emailDestino Email del administrador
   * @return true si se envió correctamente
   */
  public boolean enviarConfirmacionEmpresaCreada(Empresa empresa, String emailDestino) {
    log.info("Enviando confirmación de empresa creada: {} a {}",
        empresa.getNombreComercial(), emailDestino);

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      // Configurar mensaje
      helper.setFrom(emailFrom);
      helper.setTo(emailDestino);
      helper.setSubject("🎉 ¡Bienvenido a NathBit POS! - Empresa creada exitosamente");

      // Generar HTML
      String htmlContent = generarHtmlBienvenida(empresa);
      helper.setText(htmlContent, true);

      // Enviar
      mailSender.send(message);

      log.info("Email de bienvenida enviado exitosamente a {}", emailDestino);
      return true;

    } catch (Exception e) {
      log.error("Error enviando email de bienvenida: {}", e.getMessage(), e);
      return false;
    }
  }

  /**
   * Determina el tipo de error para decidir si reintentar
   */
  private String determinarTipoError(Exception e) {
    String mensaje = e.getMessage();

    // Errores de autenticación - no reintentar
    if (mensaje != null && (mensaje.contains("Authentication") || mensaje.contains("535"))) {
      return "AUTENTICACION";
    }

    // Errores de red/timeout - reintentar
    if (mensaje != null && (mensaje.contains("timeout") || mensaje.contains("Connection"))) {
      return "TRANSITORIO";
    }

    // Por defecto considerar transitorio
    return "TRANSITORIO";
  }

  /**
   * Reintenta enviar emails fallidos
   */
  @Transactional
  public void reintentarEmailsFallidos() {
    List<EstadoEmail> estadosReintentables = List.of(EstadoEmail.ERROR, EstadoEmail.REINTENTANDO);
    List<EmailAuditLog> pendientes = auditLogRepository.findByEstadoInAndIntentosLessThan(
        estadosReintentables, 3);

    log.info("Encontrados {} emails para reintentar", pendientes.size());

    for (EmailAuditLog auditLog : pendientes) {
      try {
        // Obtener factura y reconstruir DTO
        Factura factura = facturaRepository.findById(auditLog.getFacturaId())
            .orElseThrow(() -> new IllegalStateException(
                "Factura no encontrada: " + auditLog.getFacturaId()));

        EmailFacturaDto dto = reconstruirEmailDto(factura, auditLog);

        // Reintentar
        enviarFacturaElectronica(dto);

      } catch (Exception e) {
        log.error("Error reintentando email para factura {}: {}", auditLog.getClave(),
            e.getMessage());
      }
    }
  }

  /**
   * Envía notificación de error de factura electrónica
   *
   * @param factura      La factura que falló
   * @param mensajeError El mensaje de error de Hacienda
   * @param xmlRespuesta El XML de respuesta de Hacienda
   * @return true si se envió correctamente
   */
  public boolean enviarErrorFacturaElectronica(Factura factura, String mensajeError,
      byte[] xmlRespuesta) {
    log.info("Enviando notificación de error para factura: {} - Error: {}",
        factura.getClave(), mensajeError);

    try {
      // Obtener empresa y email de notificaciones
      Empresa empresa = factura.getSucursal().getEmpresa();
      String emailDestino = empresa.getEmailNotificacion();

      // Si no hay email de notificaciones, usar el principal
      if (emailDestino == null || emailDestino.isBlank()) {
        emailDestino = empresa.getEmail();
      }

      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      // Configurar mensaje
      helper.setFrom(emailFrom);
      helper.setTo(emailDestino);
      helper.setSubject("⚠️ Error en Factura Electrónica - " + factura.getConsecutivo());

      // Generar HTML
      String htmlContent = generarHtmlErrorFactura(factura, mensajeError);
      helper.setText(htmlContent, true);

      // Adjuntar PDF si existe
      try {
        byte[] pdfBytes = facturaPdfService.generarFacturaCarta(factura.getClave());
        if (pdfBytes != null) {
          helper.addAttachment(
              String.format("Factura_%s.pdf", factura.getClave()),
              new ByteArrayDataSource(pdfBytes, "application/pdf")
          );
        }
      } catch (Exception e) {
        log.warn("No se pudo adjuntar PDF al email de error: {}", e.getMessage());
      }

      // Adjuntar XML de respuesta si existe
      if (xmlRespuesta != null) {
        helper.addAttachment(
            String.format("Respuesta_Error_%s.xml", factura.getClave()),
            new ByteArrayDataSource(xmlRespuesta, "text/xml")
        );
      }

      // Enviar
      mailSender.send(message);

      log.info("Email de error enviado exitosamente a {}", emailDestino);
      return true;

    } catch (Exception e) {
      log.error("Error enviando email de error de factura: {}", e.getMessage(), e);
      return false;
    }
  }

  /**
   * Envía notificación de error de factura con auditoría completa
   *
   * @param dto DTO con datos del error
   * @return true si se envió correctamente
   */
  @Transactional
  public boolean enviarErrorFacturaConAuditoria(EmailErrorFacturaDto dto) {
    log.info("Enviando notificación de error para factura: {} - Error: {}",
        dto.getClave(), dto.getMensajeError());

    // Crear registro de auditoría
    EmailAuditLog auditLog = EmailAuditLog.builder()
        .facturaId(dto.getFacturaId())
        .clave(dto.getClave())
        .emailDestino(dto.getEmailDestino())
        .asunto("⚠️ Error en Factura Electrónica - " + dto.getConsecutivo())
        .estado(EstadoEmail.PENDIENTE)
        .intentos(0)
        .build();

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      // Configurar mensaje
      helper.setFrom(emailFrom);
      helper.setTo(dto.getEmailDestino());
      helper.setSubject(auditLog.getAsunto());

      // Generar HTML mejorado
      String htmlContent = generarHtmlErrorFacturaMejorado(dto);
      helper.setText(htmlContent, true);

      // Adjuntar archivos
      adjuntarArchivosError(helper, dto, auditLog);

      // Enviar
      mailSender.send(message);

      // Marcar como enviado
      auditLog.marcarEnviado();
      auditLogRepository.save(auditLog);

      log.info("Email de error enviado exitosamente a {}", dto.getEmailDestino());
      return true;

    } catch (Exception e) {
      log.error("Error enviando email de error: {}", e.getMessage(), e);

      // Registrar error en auditoría
      String tipoError = determinarTipoError(e);
      auditLog.registrarError(e.getMessage(), tipoError);
      auditLogRepository.save(auditLog);

      return false;
    }
  }


  /**
   * Reconstruye el DTO desde una factura para reintentos
   */
  private EmailFacturaDto reconstruirEmailDto(Factura factura, EmailAuditLog auditLog) {
    try {
      // Obtener empresa
      Empresa empresa = factura.getSucursal().getEmpresa();

      String nombreEmpresa = empresa.getNombreComercial() != null && !empresa.getNombreComercial().trim().isEmpty()
          ? empresa.getNombreComercial()
          : empresa.getNombreRazonSocial();

      // Generar PDF on-demand
      byte[] pdfBytes = null;
      try {
        pdfBytes = facturaPdfService.generarFacturaCarta(factura.getClave());
      } catch (Exception e) {
        log.warn("No se pudo generar PDF para reintento: {}", e.getMessage());
      }

      // Obtener XML firmado de S3
      byte[] xmlFirmadoBytes = null;
      try {
        String xmlFirmadoPath = s3PathBuilder.buildXmlPath(factura, TipoArchivoFactura.XML_SIGNED, nombreEmpresa);
        xmlFirmadoBytes = storageService.downloadFileAsBytes(xmlFirmadoPath);
      } catch (Exception e) {
        log.warn("No se pudo obtener XML firmado para reintento: {}", e.getMessage());
      }

      // Obtener respuesta Hacienda de S3
      byte[] respuestaBytes = null;
      try {
        String respuestaPath = s3PathBuilder.buildXmlPath(factura, TipoArchivoFactura.XML_RESPUESTA, nombreEmpresa);
        respuestaBytes = storageService.downloadFileAsBytes(respuestaPath);
      } catch (Exception e) {
        log.warn("No se pudo obtener respuesta Hacienda para reintento: {}", e.getMessage());
      }

      // URL del logo
      String logoUrl = null;
      try {
        if (empresa.getLogoUrl() != null && !empresa.getLogoUrl().isEmpty()) {
          // CAMBIO 4: Extraer key si es necesario antes de generar URL firmada
          String logoKey = extraerKeyDeUrl(empresa.getLogoUrl());
          if (logoKey != null) {
            logoUrl = storageService.generateSignedUrl(logoKey, 60);
          }
        }
      } catch (Exception e) {
        log.warn("No se pudo obtener URL del logo: {}", e.getMessage());
      }

      // Reconstruir DTO
      return EmailFacturaDto.builder()
          .facturaId(factura.getId())
          .clave(factura.getClave())
          .consecutivo(factura.getConsecutivo())
          .emailDestino(auditLog.getEmailDestino())
          .tipoDocumento(factura.getTipoDocumento().getDescripcion())
          .nombreComercial(empresa.getNombreComercial())
          .razonSocial(empresa.getNombreRazonSocial())
          .cedulaJuridica(empresa.getIdentificacion())
          .fechaEmision(factura.getFechaEmision())
          .logoUrl(logoUrl)
          .pdfBytes(pdfBytes)
          .xmlFirmadoBytes(xmlFirmadoBytes)
          .respuestaHaciendaBytes(respuestaBytes)
          .build();

    } catch (Exception e) {
      log.error("Error reconstruyendo EmailFacturaDto: {}", e.getMessage(), e);
      throw new RuntimeException("No se pudo reconstruir el DTO para reintento", e);
    }
  }

  /**
   * Método auxiliar para extraer la key S3 de una URL completa
   * Similar al que existe en EmpresaServiceImpl
   */
  private String extraerKeyDeUrl(String url) {
    if (url == null || url.isEmpty()) {
      return null;
    }
    // Si ya es una key (no empieza con http), devolverla tal cual
    if (!url.startsWith("http")) {
      return url;
    }

    // Buscar el patrón NathBit-POS/ que marca el inicio de la key
    int startIndex = url.indexOf("NathBit-POS/");
    if (startIndex != -1) {
      return url.substring(startIndex);
    }

    // Plan B: buscar después del bucket name
    String bucketPattern = "/snn-soluciones/";
    startIndex = url.indexOf(bucketPattern);
    if (startIndex != -1) {
      return url.substring(startIndex + bucketPattern.length());
    }

    // Si no se puede extraer, devolver null
    log.warn("No se pudo extraer key S3 de URL: {}", url);
    return null;
  }

  /**
   * Genera el HTML para el email de bienvenida
   */
  private String generarHtmlBienvenida(Empresa empresa) {
    StringBuilder html = new StringBuilder();

    html.append("<!DOCTYPE html>");
    html.append("<html>");
    html.append("<head>");
    html.append("<meta charset='UTF-8'>");
    html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
    html.append("<style>");
    html.append(
        "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif; ");
    html.append("  margin: 0; padding: 0; background-color: #f5f5f5; color: #333; }");
    html.append(".container { max-width: 600px; margin: 0 auto; background-color: white; }");
    html.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); ");
    html.append("  padding: 40px 30px; text-align: center; color: white; }");
    html.append(".header h1 { margin: 0; font-size: 28px; font-weight: 300; }");
    html.append(".emoji { font-size: 48px; margin-bottom: 20px; }");
    html.append(".content { padding: 40px 30px; }");
    html.append(".info-card { background: #f8f9fa; padding: 25px; border-radius: 12px; ");
    html.append("  margin: 25px 0; border-left: 4px solid #667eea; }");
    html.append(".info-row { margin: 10px 0; display: flex; justify-content: space-between; }");
    html.append(".info-label { color: #666; font-weight: 500; }");
    html.append(".info-value { color: #333; font-weight: 600; text-align: right; }");
    html.append(".features { margin: 30px 0; }");
    html.append(".feature { padding: 15px 0; border-bottom: 1px solid #eee; }");
    html.append(".feature:last-child { border-bottom: none; }");
    html.append(".feature-icon { color: #667eea; margin-right: 10px; }");
    html.append(".cta { text-align: center; margin: 40px 0; }");
    html.append(".btn { display: inline-block; padding: 15px 40px; ");
    html.append("  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); ");
    html.append("  color: white; text-decoration: none; border-radius: 30px; ");
    html.append("  font-weight: 600; box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3); }");
    html.append(".footer { background: #f8f9fa; padding: 30px; text-align: center; ");
    html.append("  font-size: 14px; color: #666; }");
    html.append("</style>");
    html.append("</head>");
    html.append("<body>");

    // Container
    html.append("<div class='container'>");

    // Header
    html.append("<div class='header'>");
    html.append("<div class='emoji'>🎉</div>");
    html.append("<h1>¡Bienvenido a NathBit POS!</h1>");
    html.append(
        "<p style='margin-top: 10px; opacity: 0.9;'>Su empresa ha sido creada exitosamente</p>");
    html.append("</div>");

    // Content
    html.append("<div class='content'>");

    // Greeting
    html.append("<h2 style='color: #333; margin-bottom: 20px;'>Hola, ")
        .append(empresa.getNombreComercial()).append(" 👋</h2>");
    html.append("<p style='line-height: 1.6; color: #555;'>");
    html.append(
        "Nos complace informarle que su empresa ha sido registrada exitosamente en NathBit POS. ");
    html.append("Ya puede comenzar a utilizar todas las funcionalidades de nuestro sistema.</p>");

    // Info Card
    html.append("<div class='info-card'>");
    html.append("<h3 style='margin-top: 0; color: #333;'>📋 Información de la Empresa</h3>");

    html.append("<div class='info-row'>");
    html.append("<span class='info-label'>Razón Social:</span>");
    html.append("<span class='info-value'>").append(empresa.getNombreRazonSocial())
        .append("</span>");
    html.append("</div>");

    html.append("<div class='info-row'>");
    html.append("<span class='info-label'>Nombre Comercial:</span>");
    html.append("<span class='info-value'>").append(empresa.getNombreComercial()).append("</span>");
    html.append("</div>");

    html.append("<div class='info-row'>");
    html.append("<span class='info-label'>Identificación:</span>");
    html.append("<span class='info-value'>").append(empresa.getIdentificacion()).append("</span>");
    html.append("</div>");

    html.append("<div class='info-row'>");
    html.append("<span class='info-label'>Email:</span>");
    html.append("<span class='info-value'>").append(empresa.getEmail()).append("</span>");
    html.append("</div>");

    html.append("<div class='info-row'>");
    html.append("<span class='info-label'>Teléfono:</span>");
    html.append("<span class='info-value'>").append(empresa.getTelefono()).append("</span>");
    html.append("</div>");

    if (empresa.getRequiereHacienda()) {
      html.append("<div class='info-row'>");
      html.append("<span class='info-label'>Facturación Electrónica:</span>");
      html.append("<span class='info-value' style='color: #28a745;'>✅ Habilitada</span>");
      html.append("</div>");
    }
    html.append("</div>");

    // Features
    html.append("<div class='features'>");
    html.append("<h3 style='color: #333;'>🚀 Próximos Pasos</h3>");

    html.append("<div class='feature'>");
    html.append("<span class='feature-icon'>✓</span>");
    html.append("<strong>Crear Sucursales:</strong> Configure las sucursales de su empresa");
    html.append("</div>");

    html.append("<div class='feature'>");
    html.append("<span class='feature-icon'>✓</span>");
    html.append("<strong>Agregar Productos:</strong> Ingrese su catálogo de productos y servicios");
    html.append("</div>");

    html.append("<div class='feature'>");
    html.append("<span class='feature-icon'>✓</span>");
    html.append("<strong>Configurar Usuarios:</strong> Cree cuentas para sus colaboradores");
    html.append("</div>");

    if (empresa.getRequiereHacienda()) {
      html.append("<div class='feature'>");
      html.append("<span class='feature-icon'>✓</span>");
      html.append("<strong>Probar Facturación:</strong> Realice una factura de prueba");
      html.append("</div>");
    }
    html.append("</div>");

    // CTA
    html.append("<div class='cta'>");
    html.append("<a href='#' class='btn'>Ingresar al Sistema</a>");
    html.append("</div>");

    // Support info
    html.append(
        "<div style='background: #e8f4fd; padding: 20px; border-radius: 8px; margin-top: 30px;'>");
    html.append("<h4 style='margin-top: 0; color: #0066cc;'>💡 ¿Necesita ayuda?</h4>");
    html.append("<p style='margin: 5px 0;'>Estamos aquí para apoyarlo:</p>");
    html.append("<p style='margin: 5px 0;'>📧 Email: soporte@nathbit.com</p>");
    html.append("<p style='margin: 5px 0;'>📞 WhatsApp: +506 8888-8888</p>");
    html.append(
        "<p style='margin: 5px 0;'>📚 <a href='#' style='color: #0066cc;'>Centro de Ayuda</a></p>");
    html.append("</div>");

    html.append("</div>"); // content

    // Footer
    html.append("<div class='footer'>");
    html.append("<p style='margin: 0 0 10px 0;'><strong>NathBit POS</strong></p>");
    html.append("<p style='margin: 0 0 10px 0;'>Sistema de Punto de Venta en la Nube</p>");
    html.append("<p style='margin: 0; font-size: 12px; color: #999;'>");
    html.append("Este es un correo automático, por favor no responder.<br>");
    html.append("© 2025 NathBit Solutions. Todos los derechos reservados.</p>");
    html.append("</div>");

    html.append("</div>"); // container
    html.append("</body>");
    html.append("</html>");

    return html.toString();
  }

  /**
   * Genera el HTML para el email de error de factura
   */
  private String generarHtmlErrorFactura(Factura factura, String mensajeError) {
    StringBuilder html = new StringBuilder();

    // Formatear fecha y hora
    String fechaHora = factura.getFechaEmision();

    html.append("<!DOCTYPE html>");
    html.append("<html>");
    html.append("<head>");
    html.append("<meta charset='UTF-8'>");
    html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
    html.append("<style>");
    html.append(
        "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; ");
    html.append("  margin: 0; padding: 0; background-color: #f5f5f5; color: #333; }");
    html.append(".container { max-width: 600px; margin: 0 auto; background-color: white; }");
    html.append(".header { background: linear-gradient(135deg, #dc3545 0%, #c82333 100%); ");
    html.append("  padding: 30px; text-align: center; color: white; }");
    html.append(".header h1 { margin: 0; font-size: 24px; font-weight: 600; }");
    html.append(".warning-icon { font-size: 48px; margin-bottom: 15px; }");
    html.append(".content { padding: 30px; }");
    html.append(".error-box { background: #fee; border: 1px solid #fcc; ");
    html.append("  border-radius: 8px; padding: 20px; margin: 20px 0; }");
    html.append(".error-box h3 { color: #dc3545; margin-top: 0; }");
    html.append(".info-table { width: 100%; border-collapse: collapse; margin: 20px 0; }");
    html.append(".info-table td { padding: 10px 0; border-bottom: 1px solid #eee; }");
    html.append(".info-table td:first-child { font-weight: 600; color: #666; width: 40%; }");
    html.append(".footer { background-color: #f8f9fa; padding: 20px; text-align: center; ");
    html.append("  font-size: 12px; color: #666; border-top: 1px solid #dee2e6; }");
    html.append(".action-box { background: #e3f2fd; border: 1px solid #bbdefb; ");
    html.append("  border-radius: 8px; padding: 20px; margin: 20px 0; }");
    html.append(".action-box h4 { color: #1976d2; margin-top: 0; }");
    html.append("</style>");
    html.append("</head>");
    html.append("<body>");
    html.append("<div class='container'>");

    // Header
    html.append("<div class='header'>");
    html.append("<div class='warning-icon'>⚠️</div>");
    html.append("<h1>Error en Factura Electrónica</h1>");
    html.append("</div>");

    // Content
    html.append("<div class='content'>");
    html.append("<p>Estimado usuario,</p>");
    html.append(
        "<p>Le informamos que la siguiente factura electrónica ha sido <strong>RECHAZADA</strong> por el Ministerio de Hacienda:</p>");

    // Información de la factura
    html.append("<table class='info-table'>");
    html.append("<tr><td>Número de Factura:</td><td>").append(factura.getConsecutivo())
        .append("</td></tr>");
    html.append("<tr><td>Clave Numérica:</td><td style='font-family: monospace; font-size: 12px;'>")
        .append(factura.getClave()).append("</td></tr>");
    html.append("<tr><td>Fecha y Hora:</td><td>").append(fechaHora).append("</td></tr>");
    html.append("<tr><td>Cliente:</td><td>").append(factura.getCliente().getRazonSocial())
        .append("</td></tr>");
    html.append("<tr><td>Monto Total:</td><td>₡")
        .append(String.format("%,.2f", factura.getTotalComprobante()))
        .append("</td></tr>");
    html.append("</table>");

    // Error box
    html.append("<div class='error-box'>");
    html.append("<h3>Mensaje de Error:</h3>");
    html.append("<p>").append(mensajeError).append("</p>");
    html.append("</div>");

    // Action box
    html.append("<div class='action-box'>");
    html.append("<h4>¿Qué debe hacer?</h4>");
    html.append("<ol>");
    html.append("<li>Revise el mensaje de error para identificar el problema</li>");
    html.append("<li>Corrija los datos según lo indicado</li>");
    html.append("<li>Vuelva a generar y enviar la factura</li>");
    html.append("<li>Si el problema persiste, contacte al soporte técnico</li>");
    html.append("</ol>");
    html.append("</div>");

    html.append("<p>Adjunto encontrará:");
    html.append("<ul>");
    if (factura != null) {
      html.append("<li>Copia de la factura en PDF (si se pudo generar)</li>");
    }
    html.append("<li>XML de respuesta del Ministerio de Hacienda con el detalle del error</li>");
    html.append("</ul>");

    html.append(
        "<p>Es importante corregir y reenviar esta factura lo antes posible para cumplir con los requisitos tributarios.</p>");

    html.append("<p>Saludos cordiales,<br>");
    html.append("<strong>Sistema NathBit POS</strong></p>");
    html.append("</div>");

    // Footer
    html.append("<div class='footer'>");
    html.append("<p>").append(factura.getSucursal().getEmpresa().getNombreRazonSocial())
        .append("<br>");
    html.append("Cédula: ").append(factura.getSucursal().getEmpresa().getIdentificacion())
        .append("<br>");
    html.append(
        "Este es un correo automático de notificación de errores del sistema de facturación electrónica.</p>");
    html.append("</div>");

    html.append("</div>");
    html.append("</body>");
    html.append("</html>");

    return html.toString();
  }

  /**
   * Adjunta archivos al email de error
   */
  private void adjuntarArchivosError(MimeMessageHelper helper, EmailErrorFacturaDto dto,
      EmailAuditLog auditLog)
      throws MessagingException {

    // PDF si existe
    if (dto.getPdfBytes() != null && dto.getPdfBytes().length > 0) {
      helper.addAttachment(
          String.format("Factura_Error_%s.pdf", dto.getClave()),
          new ByteArrayDataSource(dto.getPdfBytes(), "application/pdf")
      );
      auditLog.setAdjuntoPdfSize((long) dto.getPdfBytes().length);
    }

    // XML de respuesta
    if (dto.getXmlRespuestaBytes() != null && dto.getXmlRespuestaBytes().length > 0) {
      helper.addAttachment(
          String.format("Respuesta_Hacienda_Error_%s.xml", dto.getClave()),
          new ByteArrayDataSource(dto.getXmlRespuestaBytes(), "text/xml")
      );
      auditLog.setAdjuntoRespuestaSize((long) dto.getXmlRespuestaBytes().length);
    }
  }

  /**
   * HTML mejorado para errores
   */
  private String generarHtmlErrorFacturaMejorado(EmailErrorFacturaDto dto) {
    // Mapeo de códigos de error a mensajes amigables
    Map<String, String> mensajesAmigables = new HashMap<>();
    mensajesAmigables.put("01", "El emisor no está registrado como contribuyente");
    mensajesAmigables.put("02", "El receptor no está registrado");
    mensajesAmigables.put("03", "Error en el formato del documento");
    mensajesAmigables.put("04", "Error en la firma digital");
    mensajesAmigables.put("05", "Documento duplicado");
    // Agregar más códigos según documentación de Hacienda

    String mensajeAmigable = mensajesAmigables.getOrDefault(
        dto.getCodigoError(),
        dto.getMensajeError()
    );

    StringBuilder html = new StringBuilder();

    html.append("<!DOCTYPE html>");
    html.append("<html>");
    html.append("<head>");
    html.append("<meta charset='UTF-8'>");
    html.append("<style>");
    // Estilos modernos y responsivos
    html.append(
        "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; ");
    html.append(
        "  margin: 0; padding: 0; background-color: #f5f5f5; color: #333; line-height: 1.6; }");
    html.append(".container { max-width: 600px; margin: 20px auto; background-color: white; ");
    html.append("  border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
    html.append(".header { background: linear-gradient(135deg, #dc3545 0%, #bd2130 100%); ");
    html.append("  padding: 40px 30px; text-align: center; color: white; }");
    html.append(".header h1 { margin: 0; font-size: 26px; font-weight: 600; }");
    html.append(".header p { margin: 10px 0 0; opacity: 0.9; font-size: 14px; }");
    html.append(".icon { font-size: 60px; margin-bottom: 20px; }");
    html.append(".content { padding: 40px 30px; }");
    html.append(".alert { background: #fff5f5; border-left: 4px solid #dc3545; ");
    html.append("  padding: 15px 20px; margin: 20px 0; border-radius: 4px; }");
    html.append(".info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; ");
    html.append("  margin: 25px 0; padding: 20px; background: #f8f9fa; border-radius: 6px; }");
    html.append(".info-item { font-size: 14px; }");
    html.append(".info-label { font-weight: 600; color: #6c757d; margin-bottom: 5px; }");
    html.append(".info-value { color: #212529; }");
    html.append(".clave { font-family: 'Courier New', monospace; font-size: 11px; ");
    html.append(
        "  word-break: break-all; background: #e9ecef; padding: 5px; border-radius: 3px; }");
    html.append(
        ".steps { background: #e8f5e9; padding: 20px; border-radius: 6px; margin: 20px 0; }");
    html.append(".steps h3 { color: #2e7d32; margin-top: 0; }");
    html.append(".steps ol { margin: 10px 0; padding-left: 20px; }");
    html.append(".steps li { margin: 8px 0; }");
    html.append(".footer { background-color: #f8f9fa; padding: 30px; text-align: center; ");
    html.append("  font-size: 12px; color: #6c757d; border-top: 1px solid #dee2e6; }");
    html.append(".button { display: inline-block; padding: 10px 20px; background: #007bff; ");
    html.append("  color: white; text-decoration: none; border-radius: 5px; margin: 10px 0; }");
    html.append("@media (max-width: 600px) { ");
    html.append("  .info-grid { grid-template-columns: 1fr; }");
    html.append("  .container { margin: 0; border-radius: 0; }");
    html.append("}");
    html.append("</style>");
    html.append("</head>");
    html.append("<body>");
    html.append("<div class='container'>");

    // Header mejorado
    html.append("<div class='header'>");
    html.append("<div class='icon'>⚠️</div>");
    html.append("<h1>Factura Rechazada por Hacienda</h1>");
    html.append("<p>Requiere corrección inmediata</p>");
    html.append("</div>");

    // Content
    html.append("<div class='content'>");

    // Alert
    html.append("<div class='alert'>");
    html.append("<strong>Mensaje de Hacienda:</strong><br>");
    html.append(mensajeAmigable);
    html.append("</div>");

    // Grid de información
    html.append("<div class='info-grid'>");

    html.append("<div class='info-item'>");
    html.append("<div class='info-label'>Número de Factura</div>");
    html.append("<div class='info-value'>").append(dto.getConsecutivo()).append("</div>");
    html.append("</div>");

    html.append("<div class='info-item'>");
    html.append("<div class='info-label'>Fecha y Hora</div>");
    html.append("<div class='info-value'>").append(dto.getFechaEmision()).append("</div>");
    html.append("</div>");

    html.append("<div class='info-item'>");
    html.append("<div class='info-label'>Cliente</div>");
    html.append("<div class='info-value'>").append(dto.getNombreCliente()).append("</div>");
    html.append("</div>");

    html.append("<div class='info-item'>");
    html.append("<div class='info-label'>Monto Total</div>");
    html.append("<div class='info-value' style='font-weight: bold; color: #dc3545;'>")
        .append(dto.getMontoTotal()).append("</div>");
    html.append("</div>");

    html.append("</div>");

    // Clave
    html.append("<div class='info-item' style='margin: 20px 0;'>");
    html.append("<div class='info-label'>Clave Numérica</div>");
    html.append("<div class='clave'>").append(dto.getClave()).append("</div>");
    html.append("</div>");

    // Pasos a seguir
    html.append("<div class='steps'>");
    html.append("<h3>Pasos a Seguir</h3>");
    html.append("<ol>");
    html.append("<li>Revise el archivo XML adjunto para ver el detalle del error</li>");
    html.append("<li>Corrija los datos según el error indicado</li>");
    html.append("<li>Genere nuevamente la factura con los datos corregidos</li>");
    html.append("<li>Si necesita ayuda, contacte al soporte técnico</li>");
    html.append("</ol>");
    html.append("</div>");

    html.append("</div>");

    // Footer
    html.append("<div class='footer'>");
    html.append("<strong>").append(dto.getNombreEmpresa()).append("</strong><br>");
    html.append("Cédula: ").append(dto.getCedulaEmpresa()).append("<br><br>");
    html.append("Sistema de Facturación Electrónica - NathBit POS<br>");
    html.append("<small>Este es un mensaje automático, no responder a este correo</small>");
    html.append("</div>");

    html.append("</div>");
    html.append("</body>");
    html.append("</html>");

    return html.toString();
  }

  /**
   * Enviar email simple sin adjuntos (para notificaciones)
   */
  public void enviarEmailSimple(String destinatario, String asunto, String mensaje) {
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

      helper.setFrom(emailFrom);
      helper.setTo(destinatario);
      helper.setSubject(asunto);

      // HTML simple
      String htmlContent = construirHtmlSimple(mensaje);
      helper.setText(htmlContent, true);

      mailSender.send(mimeMessage);
      log.info("Email simple enviado a: {}", destinatario);

    } catch (MessagingException e) {
      log.error("Error enviando email simple a {}: {}", destinatario, e.getMessage());
      throw new RuntimeException("Error al enviar email: " + e.getMessage(), e);
    }
  }

  /**
   * Construir HTML básico para emails simples
   */
  private String construirHtmlSimple(String mensaje) {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { 
                    font-family: Arial, sans-serif; 
                    color: #333; 
                    line-height: 1.6; 
                }
                .container { 
                    max-width: 600px; 
                    margin: 0 auto; 
                    padding: 20px; 
                    background-color: #f9f9f9; 
                }
                .content { 
                    background: white; 
                    padding: 30px; 
                    border-radius: 8px; 
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1); 
                }
                .header { 
                    color: #2c3e50; 
                    border-bottom: 2px solid #3498db; 
                    padding-bottom: 10px; 
                    margin-bottom: 20px; 
                }
                .footer { 
                    margin-top: 30px; 
                    padding-top: 20px; 
                    border-top: 1px solid #ddd; 
                    font-size: 12px; 
                    color: #666; 
                    text-align: center; 
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="content">
                    <div class="header">
                        <h2>NathBit POS - Notificación del Sistema</h2>
                    </div>
                    <div>
                        %s
                    </div>
                    <div class="footer">
                        Este es un email automático de NathBit POS. Por favor no responder.
                    </div>
                </div>
            </div>
        </body>
        </html>
        """.formatted(mensaje.replace("\n", "<br>"));
  }
}