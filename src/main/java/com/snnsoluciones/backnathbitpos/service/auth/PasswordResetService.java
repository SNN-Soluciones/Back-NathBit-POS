package com.snnsoluciones.backnathbitpos.service.auth;

import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.EmailService;
import com.snnsoluciones.backnathbitpos.service.ResendEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UsuarioRepository usuarioRepository;
    private final ResendEmailService resendEmailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void solicitarReset(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("No existe una cuenta con ese email"));

        // Generar código de 6 dígitos
        String codigo = String.format("%06d", new Random().nextInt(999999));

        // Guardar en usuario con expiración de 15 minutos
        usuario.setResetToken(codigo);
        usuario.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        usuarioRepository.save(usuario);

        // Enviar email
        String html = generarHtmlReset(usuario.getNombre(), codigo);
        boolean enviado = resendEmailService.enviar(email, "🔐 Código para restablecer tu contraseña - NathBit POS", html);

        if (!enviado) {
            throw new RuntimeException("Error al enviar el email. Intente nuevamente.");
        }

        log.info("Código de reset enviado a: {}", email);
    }

    @Transactional
    public void resetPassword(String email, String codigo, String nuevaPassword, String confirmarPassword) {
        if (!nuevaPassword.equals(confirmarPassword)) {
            throw new RuntimeException("Las contraseñas no coinciden");
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (usuario.getResetToken() == null || !usuario.getResetToken().equals(codigo)) {
            throw new RuntimeException("Código inválido");
        }

        if (usuario.getResetTokenExpiry() == null || LocalDateTime.now().isAfter(usuario.getResetTokenExpiry())) {
            throw new RuntimeException("El código ha expirado. Solicitá uno nuevo.");
        }

        // Cambiar password y limpiar token
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuario.setResetToken(null);
        usuario.setResetTokenExpiry(null);
        usuario.setRequiereCambioPassword(false);
        usuarioRepository.save(usuario);

        log.info("Password reseteado exitosamente para: {}", email);
    }

    private String generarHtmlReset(String nombre, String codigo) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f5f5f5; margin: 0; padding: 20px;">
                <div style="max-width: 500px; margin: 0 auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1);">
                    <div style="background: linear-gradient(135deg, #7C3AED, #8B5CF6); padding: 30px; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 22px;">NathBit POS</h1>
                        <p style="color: rgba(255,255,255,0.85); margin: 8px 0 0 0;">Restablecer contraseña</p>
                    </div>
                    <div style="padding: 35px 30px;">
                        <p style="color: #333; margin-top: 0;">Hola <strong>%s</strong>,</p>
                        <p style="color: #555;">Recibimos una solicitud para restablecer tu contraseña. Usá el siguiente código:</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <div style="background: linear-gradient(135deg, #7C3AED, #8B5CF6); color: white; font-size: 36px; font-weight: bold; letter-spacing: 10px; padding: 20px 30px; border-radius: 10px; display: inline-block;">
                                %s
                            </div>
                            <p style="color: #dc3545; margin-top: 15px; font-size: 14px;">⏱️ Expira en <strong>15 minutos</strong></p>
                        </div>
                        <div style="background: #fff3cd; border-radius: 8px; padding: 15px; margin-top: 10px;">
                            <p style="margin: 0; color: #856404; font-size: 14px;">⚠️ Si no solicitaste esto, ignorá este mensaje. Tu contraseña no cambiará.</p>
                        </div>
                    </div>
                    <div style="background: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #999;">
                        Este es un mensaje automático de NathBit POS. Por favor no responder.
                    </div>
                </div>
            </body>
            </html>
            """.formatted(nombre, codigo);
    }
}