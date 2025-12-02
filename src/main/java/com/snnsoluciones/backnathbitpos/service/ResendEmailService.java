package com.snnsoluciones.backnathbitpos.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.Attachment;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class ResendEmailService {

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from-email:noreply@nathbit.com}")
    private String fromEmail;

    @Value("${resend.from-name:NathBit POS}")
    private String fromName;

    private Resend resend;

    @PostConstruct
    public void init() {
        this.resend = new Resend(apiKey);
        log.info("✅ Resend inicializado con email: {}", fromEmail);
    }

    /**
     * Enviar email simple (sin adjuntos)
     */
    public boolean enviar(String to, String subject, String htmlContent) {
        try {
            CreateEmailOptions options = CreateEmailOptions.builder()
                .from(fromName + " <" + fromEmail + ">")
                .to(to)
                .subject(subject)
                .html(htmlContent)
                .build();

            CreateEmailResponse response = resend.emails().send(options);
            log.info("✅ Email enviado a {} - ID: {}", to, response.getId());
            return true;

        } catch (ResendException e) {
            log.error("❌ Error enviando email a {}: {}", to, e.getMessage());
            return false;
        }
    }

    /**
     * Enviar email con adjuntos
     */
    public boolean enviarConAdjuntos(String to, String subject, String htmlContent, 
                                      List<EmailAttachment> adjuntos) {
        try {
            CreateEmailOptions.Builder builder = CreateEmailOptions.builder()
                .from(fromName + " <" + fromEmail + ">")
                .to(to)
                .subject(subject)
                .html(htmlContent);

            // Agregar adjuntos
            if (adjuntos != null && !adjuntos.isEmpty()) {
                List<Attachment> attachments = adjuntos.stream()
                    .map(adj -> Attachment.builder()
                        .fileName(adj.nombre())
                        .content(Base64.getEncoder().encodeToString(adj.contenido()))
                        .build())
                    .toList();
                builder.attachments(attachments);
            }

            CreateEmailResponse response = resend.emails().send(builder.build());
            log.info("✅ Email con {} adjuntos enviado a {} - ID: {}", 
                adjuntos != null ? adjuntos.size() : 0, to, response.getId());
            return true;

        } catch (ResendException e) {
            log.error("❌ Error enviando email con adjuntos a {}: {}", to, e.getMessage());
            return false;
        }
    }

    /**
     * Record para adjuntos
     */
    public record EmailAttachment(String nombre, byte[] contenido, String tipo) {}
}