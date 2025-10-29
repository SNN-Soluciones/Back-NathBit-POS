package com.snnsoluciones.backnathbitpos.service;

import jakarta.mail.*;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.RecipientStringTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SubjectTerm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * Servicio para validar emails enviados consultando Gmail vía IMAP.
 * Se usa como validación de respaldo cuando no hay registro en email_audit_log.
 *
 * Este servicio busca en la carpeta "Sent" de Gmail para verificar
 * si un email con una clave específica ya fue enviado a un destinatario.
 */
@Slf4j
@Service
public class ImapService {

    @Value("${spring.mail.username}")
    private String emailUsername;

    @Value("${spring.mail.password}")
    private String emailPassword;

    @Value("${imap.host:imap.gmail.com}")
    private String imapHost;

    @Value("${imap.port:993}")
    private int imapPort;

    @Value("${imap.timeout:10000}") // 10 segundos default
    private int imapTimeout;

    /**
     * Busca en la carpeta "Sent" de Gmail si existe un email enviado 
     * con la clave en el asunto y al email destino especificado.
     *
     * @param clave Clave de la factura (debe estar en el asunto del email)
     * @param emailDestino Email del destinatario
     * @return true si encuentra el email en Sent, false en caso contrario
     */
    public boolean emailExisteEnEnviados(String clave, String emailDestino) {
        Store store = null;
        Folder sentFolder = null;

        try {
            log.debug("🔍 Buscando en IMAP: clave={}, destino={}", clave, emailDestino);

            // 1. Configurar propiedades IMAP
            Properties props = new Properties();
            props.setProperty("mail.store.protocol", "imaps");
            props.setProperty("mail.imaps.host", imapHost);
            props.setProperty("mail.imaps.port", String.valueOf(imapPort));
            props.setProperty("mail.imaps.connectiontimeout", String.valueOf(imapTimeout));
            props.setProperty("mail.imaps.timeout", String.valueOf(imapTimeout));
            props.setProperty("mail.imaps.ssl.enable", "true");
            props.setProperty("mail.imaps.ssl.trust", "*");

            // 2. Conectar al servidor
            Session session = Session.getInstance(props, null);
            store = session.getStore("imaps");
            store.connect(imapHost, emailUsername, emailPassword);

            // 3. Abrir carpeta "Sent" 
            sentFolder = abrirCarpetaEnviados(store);

            if (sentFolder == null) {
                log.warn("⚠️ No se pudo abrir carpeta Sent en Gmail");
                return false;
            }

            // 4. Buscar emails que coincidan con clave + destino
            SearchTerm claveEnAsunto = new SubjectTerm(clave);
            SearchTerm destinatarioCoincide = new RecipientStringTerm(Message.RecipientType.TO, emailDestino);
            SearchTerm criterioCompleto = new AndTerm(claveEnAsunto, destinatarioCoincide);

            Message[] mensajesEncontrados = sentFolder.search(criterioCompleto);

            boolean encontrado = mensajesEncontrados != null && mensajesEncontrados.length > 0;

            if (encontrado) {
                log.info("✅ IMAP: Email encontrado en Sent - clave={}, destino={}, cantidad={}",
                    clave, emailDestino, mensajesEncontrados.length);
            } else {
                log.debug("❌ IMAP: Email NO encontrado en Sent - clave={}, destino={}",
                    clave, emailDestino);
            }

            return encontrado;

        } catch (MessagingException e) {
            log.error("❌ Error conectando a IMAP para buscar email: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("❌ Error inesperado buscando en IMAP: {}", e.getMessage(), e);
            return false;
        } finally {
            // 5. Cerrar conexiones
            cerrarRecursos(sentFolder, store);
        }
    }

    /**
     * Intenta abrir la carpeta "Sent" probando diferentes nombres 
     * (Gmail usa nombres distintos según idioma del usuario)
     */
    private Folder abrirCarpetaEnviados(Store store) throws MessagingException {
        // Gmail puede usar diferentes nombres para la carpeta Sent
        String[] posiblesNombres = {
            "[Gmail]/Sent Mail",  // Inglés (más común)
            "[Gmail]/Enviados",   // Español
            "Sent",               // Fallback genérico
            "Sent Messages"       // Otro posible nombre
        };

        for (String nombreCarpeta : posiblesNombres) {
            try {
                Folder folder = store.getFolder(nombreCarpeta);
                if (folder.exists()) {
                    folder.open(Folder.READ_ONLY);
                    log.debug("✅ Carpeta Sent abierta: {}", nombreCarpeta);
                    return folder;
                }
            } catch (MessagingException e) {
                log.debug("❌ No se pudo abrir carpeta '{}': {}", nombreCarpeta, e.getMessage());
            }
        }

        return null;
    }

    /**
     * Cierra folder y store de forma segura
     */
    private void cerrarRecursos(Folder folder, Store store) {
        try {
            if (folder != null && folder.isOpen()) {
                folder.close(false);
            }
        } catch (MessagingException e) {
            log.warn("Error cerrando folder IMAP: {}", e.getMessage());
        }

        try {
            if (store != null && store.isConnected()) {
                store.close();
            }
        } catch (MessagingException e) {
            log.warn("Error cerrando store IMAP: {}", e.getMessage());
        }
    }
}