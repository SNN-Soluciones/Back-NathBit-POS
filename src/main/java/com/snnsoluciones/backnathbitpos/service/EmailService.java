package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.email.EmailFacturaDto;
import com.snnsoluciones.backnathbitpos.entity.EmailAuditLog;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.entity.FacturaDocumento;
import com.snnsoluciones.backnathbitpos.enums.EstadoEmail;
import com.snnsoluciones.backnathbitpos.enums.facturacion.TipoArchivoFactura;
import com.snnsoluciones.backnathbitpos.repository.EmailAuditLogRepository;
import com.snnsoluciones.backnathbitpos.repository.FacturaDocumentoRepository;
import com.snnsoluciones.backnathbitpos.repository.FacturaRepository;
import com.snnsoluciones.backnathbitpos.service.pdf.FacturaPdfService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para envío de facturas electrónicas por email
 * Maneja plantillas HTML, adjuntos múltiples y auditoría
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailAuditLogRepository auditLogRepository;
    private final FacturaRepository facturaRepository;
    private final StorageService storageService;
    private final FacturaPdfService facturaPdfService;
    private final FacturaDocumentoRepository documentoRepository;

    @Value("${spring.mail.username}")
    private String emailFrom;

    @Value("${app.email.max-attachment-size:10485760}") // 10MB default
    private Long maxAttachmentSize;

    /**
     * Envía factura electrónica con todos los adjuntos
     *
     * @param dto Datos del email a enviar
     * @return EmailAuditLog con el resultado
     */
    @Transactional
    public EmailAuditLog enviarFacturaElectronica(EmailFacturaDto dto) {
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

        return auditLogRepository.save(auditLog);
    }

    /**
     * Construye el mensaje MIME con HTML y adjuntos
     */
    private MimeMessage construirMensaje(EmailFacturaDto dto, EmailAuditLog auditLog) throws MessagingException {
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
        html.append("body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background-color: white; }");
        html.append(".header { background-color: #f8f9fa; padding: 30px; text-align: center; border-bottom: 3px solid #007bff; }");
        html.append(".logo { max-height: 80px; margin-bottom: 15px; }");
        html.append(".content { padding: 30px; }");
        html.append(".info-box { background: #e8f4fd; padding: 20px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #007bff; }");
        html.append(".footer { background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #666; }");
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
        html.append("<p>Le enviamos su comprobante electrónico correspondiente a la compra realizada en nuestro establecimiento.</p>");

        // Info box
        html.append("<div class='info-box'>");
        html.append("<strong>Tipo de documento:</strong> ").append(dto.getTipoDocumento()).append("<br>");
        html.append("<strong>Número consecutivo:</strong> ").append(dto.getConsecutivo()).append("<br>");
        html.append("<strong>Clave numérica:</strong> ").append(dto.getClave()).append("<br>");
        html.append("<strong>Fecha:</strong> ").append(dto.getFechaEmision()).append("<br>");
        html.append("</div>");

        html.append("<p><strong>Documentos adjuntos:</strong></p>");
        html.append("<ul>");
        html.append("<li>Factura en formato PDF</li>");
        html.append("<li>Comprobante XML firmado digitalmente</li>");
        html.append("<li>Respuesta de validación del Ministerio de Hacienda</li>");
        html.append("</ul>");

        html.append("<p>Conserve estos documentos para sus registros. El comprobante electrónico tiene la misma validez que el documento físico.</p>");
        html.append("<p>Agradecemos su preferencia.</p>");
        html.append("<p>Saludos cordiales,<br><strong>").append(dto.getNombreComercial()).append("</strong></p>");
        html.append("</div>");

        // Footer
        html.append("<div class='footer'>");
        html.append("<p>Este es un correo automático, por favor no responder.<br>");
        html.append(dto.getRazonSocial()).append(" - Cédula Jurídica: ").append(dto.getCedulaJuridica()).append("<br>");
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
    private void adjuntarArchivos(MimeMessageHelper helper, EmailFacturaDto dto, EmailAuditLog auditLog) throws MessagingException {
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
        List<EmailAuditLog> pendientes = auditLogRepository.findByEstadoInAndIntentosLessThan(estadosReintentables, 3);

        log.info("Encontrados {} emails para reintentar", pendientes.size());

        for (EmailAuditLog auditLog : pendientes) {
            try {
                // Obtener factura y reconstruir DTO
                Factura factura = facturaRepository.findById(auditLog.getFacturaId())
                    .orElseThrow(() -> new IllegalStateException("Factura no encontrada: " + auditLog.getFacturaId()));

                EmailFacturaDto dto = reconstruirEmailDto(factura, auditLog);

                // Reintentar
                enviarFacturaElectronica(dto);

            } catch (Exception e) {
                log.error("Error reintentando email para factura {}: {}", auditLog.getClave(), e.getMessage());
            }
        }
    }

    /**
     * Reconstruye el DTO desde una factura para reintentos
     */
    private EmailFacturaDto reconstruirEmailDto(Factura factura, EmailAuditLog auditLog) {
        try {
            // Obtener empresa
            Empresa empresa = factura.getSucursal().getEmpresa();

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
                xmlFirmadoBytes = storageService.downloadFileAsBytes(
                    documentoRepository.findOneByClaveAndTipoArchivo(
                        factura.getClave(),
                        TipoArchivoFactura.XML_SIGNED
                    ).map(FacturaDocumento::getS3Key).orElse(null)
                );
            } catch (Exception e) {
                log.warn("No se pudo obtener XML firmado para reintento: {}", e.getMessage());
            }

            // Obtener respuesta Hacienda de S3
            byte[] respuestaBytes = null;
            try {
                respuestaBytes = storageService.downloadFileAsBytes(
                    documentoRepository.findOneByClaveAndTipoArchivo(
                        factura.getClave(),
                        TipoArchivoFactura.XML_RESPUESTA
                    ).map(FacturaDocumento::getS3Key).orElse(null)
                );
            } catch (Exception e) {
                log.warn("No se pudo obtener respuesta Hacienda para reintento: {}", e.getMessage());
            }

            // URL del logo
            String logoUrl = null;
            try {
                if (empresa.getLogoUrl() != null) {
                    logoUrl = storageService.generateSignedUrl(empresa.getLogoUrl(), 60);
                }
            } catch (Exception e) {
                log.warn("No se pudo obtener URL del logo: {}", e.getMessage());
            }

            // Reconstruir DTO
            return EmailFacturaDto.builder()
                .facturaId(factura.getId())
                .clave(factura.getClave())
                .consecutivo(factura.getConsecutivo())
                .emailDestino(auditLog.getEmailDestino()) // Usar el email del log
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
}