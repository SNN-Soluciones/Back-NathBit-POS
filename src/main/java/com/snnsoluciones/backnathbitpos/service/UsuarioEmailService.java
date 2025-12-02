package com.snnsoluciones.backnathbitpos.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioEmailService {

    private final ResendEmailService resendEmailService;

    @Value("${app.nombre-sistema:NathBit POS}")
    private String nombreSistema;

    public void enviarCredencialesTemporal(String email, String nombreUsuario, String passwordTemporal) {
        try {
            String asunto = nombreSistema + " - Credenciales de Acceso";
            String contenido = buildEmailContent(nombreUsuario, email, passwordTemporal);

            boolean enviado = resendEmailService.enviar(email, asunto, contenido);

            if (enviado) {
                log.info("✅ Email de credenciales enviado a: {}", email);
            } else {
                log.warn("⚠️ No se pudo enviar email a: {}", email);
            }

        } catch (Exception e) {
            log.error("❌ Error enviando email a {}: {}", email, e.getMessage());
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
                    
                    <p style="color: #e74c3c;"><strong>Importante:</strong> Por seguridad, deberá cambiar esta contraseña en su primer inicio de sesión.</p>
                    
                    <p>Saludos,<br>Equipo de %s</p>
                </div>
            </body>
            </html>
            """.formatted(nombreSistema, nombre, email, password, nombreSistema);
    }
}