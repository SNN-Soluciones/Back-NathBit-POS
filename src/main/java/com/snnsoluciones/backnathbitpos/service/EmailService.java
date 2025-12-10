package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.email.EmailErrorFacturaDto;
import com.snnsoluciones.backnathbitpos.dto.email.EmailFacturaDto;
import com.snnsoluciones.backnathbitpos.entity.EmailAuditLog;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.enums.EstadoEmail;
import com.snnsoluciones.backnathbitpos.repository.EmailAuditLogRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.util.ByteArrayDataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

  private final ResendEmailService resendEmailService;
  private final EmailAuditLogRepository auditLogRepository;

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
      List<ResendEmailService.EmailAttachment> adjuntos = construirAdjuntos(dto);
      String htmlContent = generarHtmlFactura(dto);
      boolean enviado = resendEmailService.enviarConAdjuntos(
          dto.getEmailDestino(),
          dto.getAsunto(),
          htmlContent,
          adjuntos
      );
      if (!enviado) {
        throw new RuntimeException("Error al enviar email via Resend");
      }

      // Marcar como enviado
      auditLog.marcarEnviado();
      log.info("Email enviado exitosamente para factura {}", dto.getClave());

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
    html.append("Autorizado mediante Resolución MH-DGT-RES-0027-2024</p>");
    html.append("</div>");

    html.append("</div>");
    html.append("</body>");
    html.append("</html>");

    return html.toString();
  }

  /**
   * Envía email con código de registro de dispositivo (OTP)
   */
  public void enviarCodigoRegistroDispositivo(String destinatario,
      String nombreDestinatario,
      String nombreEmpresa,
      String nombreDispositivo,
      String codigo,
      String ipSolicitante,
      String plataforma,
      int minutosExpiracion) {
    log.info("Enviando código OTP a {} para dispositivo {}", destinatario, nombreDispositivo);

    String asunto = "🔐 Código de registro de dispositivo - " + nombreEmpresa;
    String htmlContent = generarHtmlCodigoRegistro(
        nombreDestinatario, nombreEmpresa, nombreDispositivo,
        codigo, ipSolicitante, plataforma, minutosExpiracion
    );

    boolean enviado = resendEmailService.enviar(destinatario, asunto, htmlContent);
    if (!enviado) {
      throw new RuntimeException("Error al enviar código OTP");
    }
    log.info("Código OTP enviado exitosamente a {}", destinatario);
  }

  /**
   * Genera HTML para email de código OTP
   */
  private String generarHtmlCodigoRegistro(String nombreDestinatario,
      String nombreEmpresa,
      String nombreDispositivo,
      String codigo,
      String ipSolicitante,
      String plataforma,
      int minutosExpiracion) {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>
        <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
            <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 30px; border-radius: 10px 10px 0 0;">
                <h1 style="color: white; margin: 0; font-size: 24px;">🔐 Código de Verificación</h1>
                <p style="color: rgba(255,255,255,0.9); margin: 10px 0 0 0;">%s</p>
            </div>
            
            <div style="background: #f8f9fa; padding: 30px; border: 1px solid #e9ecef; border-top: none; border-radius: 0 0 10px 10px;">
                <p style="margin-top: 0;">Hola <strong>%s</strong>,</p>
                
                <p>Un nuevo dispositivo está solicitando acceso al sistema:</p>
                
                <div style="background: white; border: 1px solid #dee2e6; border-radius: 8px; padding: 20px; margin: 20px 0;">
                    <table style="width: 100%%; border-collapse: collapse;">
                        <tr>
                            <td style="padding: 8px 0; color: #6c757d;">📱 Dispositivo:</td>
                            <td style="padding: 8px 0; font-weight: bold;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px 0; color: #6c757d;">💻 Plataforma:</td>
                            <td style="padding: 8px 0;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px 0; color: #6c757d;">🌐 IP:</td>
                            <td style="padding: 8px 0;">%s</td>
                        </tr>
                    </table>
                </div>
                
                <div style="text-align: center; margin: 30px 0;">
                    <p style="margin-bottom: 10px; color: #6c757d;">Tu código de verificación es:</p>
                    <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; font-size: 32px; font-weight: bold; letter-spacing: 8px; padding: 20px 40px; border-radius: 10px; display: inline-block;">
                        %s
                    </div>
                    <p style="margin-top: 15px; color: #dc3545; font-size: 14px;">
                        ⏱️ Este código expira en <strong>%d minutos</strong>
                    </p>
                </div>
                
                <div style="background: #fff3cd; border: 1px solid #ffc107; border-radius: 8px; padding: 15px; margin-top: 20px;">
                    <p style="margin: 0; color: #856404; font-size: 14px;">
                        <strong>⚠️ Importante:</strong> Si no reconoces esta solicitud, ignora este mensaje.
                    </p>
                </div>
                
                <hr style="border: none; border-top: 1px solid #dee2e6; margin: 30px 0;">
                
                <p style="color: #6c757d; font-size: 12px; text-align: center; margin-bottom: 0;">
                    Este es un mensaje automático de NathBit POS.<br>
                    Por favor no responda a este correo.
                </p>
            </div>
        </body>
        </html>
        """.formatted(
        nombreEmpresa,
        nombreDestinatario,
        nombreDispositivo,
        plataforma != null ? plataforma : "No especificada",
        ipSolicitante != null ? ipSolicitante : "No disponible",
        codigo,
        minutosExpiracion
    );
  }

  /**
   * Adjunta los archivos al mensaje
   */
  private List<ResendEmailService.EmailAttachment> construirAdjuntos(EmailFacturaDto dto) {
    List<ResendEmailService.EmailAttachment> adjuntos = new java.util.ArrayList<>();

    // PDF
    if (dto.getPdfBytes() != null) {
      adjuntos.add(new ResendEmailService.EmailAttachment(
          dto.getConsecutivo() + ".pdf",
          dto.getPdfBytes(),
          "application/pdf"
      ));
    }

    // XML Firmado
    if (dto.getXmlFirmadoBytes() != null) {
      adjuntos.add(new ResendEmailService.EmailAttachment(
          dto.getClave() + ".xml",
          dto.getXmlFirmadoBytes(),
          "application/xml"
      ));
    }

    // Respuesta Hacienda
    if (dto.getRespuestaHaciendaBytes() != null) {
      adjuntos.add(new ResendEmailService.EmailAttachment(
          dto.getClave() + "_respuesta.xml",
          dto.getRespuestaHaciendaBytes(),
          "application/xml"
      ));
    }

    return adjuntos;
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
      String asunto = "🎉 ¡Bienvenido a NathBit POS! - Empresa creada exitosamente";
      String htmlContent = generarHtmlBienvenida(empresa);

      boolean enviado = resendEmailService.enviar(emailDestino, asunto, htmlContent);

      if (enviado) {
        log.info("Email de bienvenida enviado exitosamente a {}", emailDestino);
      }

      return enviado;

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
   * Enviar email simple sin adjuntos (para notificaciones)
   */
  public void enviarEmailSimple(String destinatario, String asunto, String mensaje) {
    String htmlContent = construirHtmlSimple(mensaje);
    boolean enviado = resendEmailService.enviar(destinatario, asunto, htmlContent);
    if (!enviado) {
      throw new RuntimeException("Error al enviar email simple");
    }
    log.info("Email simple enviado a: {}", destinatario);
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