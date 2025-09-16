package com.snnsoluciones.backnathbitpos.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioEmailService {

    private final JavaMailSender mailSender;
    
    @Value("${app.email.from:noreply@nathbitpos.com}")
    private String emailFrom;
    
    @Value("${app.nombre-sistema:NathBit POS}")
    private String nombreSistema;

    public void enviarCredencialesTemporal(String email, String nombreUsuario, String passwordTemporal) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(email);
            helper.setFrom(emailFrom);
            helper.setSubject(nombreSistema + " - Credenciales de Acceso");
            
            String contenido = buildEmailContent(nombreUsuario, email, passwordTemporal);
            helper.setText(contenido, true);
            
            mailSender.send(message);
            log.info("Email enviado exitosamente a: {}", email);
            
        } catch (Exception e) {
            log.error("Error enviando email a {}: {}", email, e.getMessage());
            // NO lanzar excepción para no interrumpir la creación del usuario
        }
    }
    
    private String buildEmailContent(String nombre, String email, String password) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">Bienvenido a %s</h2>
                    <p>Hola <strong>%s</strong>,</p>
                    <p>Se ha creado tu cuenta en el sistema. Aquí están tus credenciales de acceso:</p>
                    
                    <div style="background: #f8f9fa; padding: 20px; border-radius: 5px; margin: 20px 0;">
                        <p><strong>Usuario:</strong> %s</p>
                        <p><strong>Contraseña temporal:</strong> %s</p>
                    </div>
                    
                    <p style="color: #e74c3c;"><strong>Importante:</strong> Por seguridad, se te pedirá cambiar esta contraseña en tu primer inicio de sesión.</p>
                    
                    <p>Saludos,<br>Equipo de %s</p>
                </div>
            </body>
            </html>
            """.formatted(nombreSistema, nombre, email, password, nombreSistema);
    }
}