// src/main/java/com/snnsoluciones/backnathbitpos/service/impl/CertificadoServiceImpl.java
package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.service.CertificadoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Enumeration;

@Slf4j
@Service
public class CertificadoServiceImpl implements CertificadoService {

    @Value("${app.encryption.master-key:YourDefaultMasterKey32BytesLong!!}")
    private String masterKey;

    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String KEYSTORE_TYPE = "PKCS12";

    @Override
    public boolean validarCertificado(MultipartFile certificadoFile, String pin) {
        try {
            log.info("Validando certificado: {}", certificadoFile.getOriginalFilename());
            log.info("Tamaño: {} bytes", certificadoFile.getSize());
            log.info("Tipo: {}", certificadoFile.getContentType());

            // Validar que el archivo no esté vacío
            if (certificadoFile.isEmpty() || certificadoFile.getSize() == 0) {
                log.error("El archivo está vacío");
                return false;
            }

            // Limpiar el PIN (por si acaso)
            String pinLimpio = pin != null ? pin.trim() : "";
            log.info("PIN length: {}", pinLimpio.length());

            // Cargar el keystore
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);

            // IMPORTANTE: Usar getBytes() en lugar de getInputStream() para evitar problemas
            byte[] certificadoBytes = certificadoFile.getBytes();
            ByteArrayInputStream bais = new ByteArrayInputStream(certificadoBytes);

            // Cargar con el PIN
            keyStore.load(bais, pinLimpio.toCharArray());

            // Verificar que el keystore tenga al menos una entrada
            Enumeration<String> aliases = keyStore.aliases();
            if (!aliases.hasMoreElements()) {
                log.error("El certificado no contiene entradas válidas");
                return false;
            }

            // Obtener el primer alias
            String alias = aliases.nextElement();
            log.info("Alias encontrado: {}", alias);

            // Verificar que sea una entrada de clave (key entry)
            if (!keyStore.isKeyEntry(alias)) {
                log.error("La entrada no es una clave privada");
                return false;
            }

            // Obtener el certificado
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
            if (cert == null) {
                log.error("No se pudo obtener el certificado");
                return false;
            }

            // Mostrar información del certificado
            log.info("Certificado para: {}", cert.getSubjectDN().getName());
            log.info("Emitido por: {}", cert.getIssuerDN().getName());
            log.info("Serial: {}", cert.getSerialNumber());

            // Verificar que no esté expirado
            try {
                cert.checkValidity();
                log.info("Certificado válido hasta: {}", cert.getNotAfter());
            } catch (Exception e) {
                log.error("El certificado está expirado o aún no es válido: {}", e.getMessage());
                return false;
            }

            // Verificar que podemos obtener la clave privada con el PIN
            try {
                Key privateKey = keyStore.getKey(alias, pinLimpio.toCharArray());
                if (privateKey == null) {
                    log.error("No se pudo obtener la clave privada");
                    return false;
                }
                log.info("Clave privada obtenida correctamente, algoritmo: {}", privateKey.getAlgorithm());
            } catch (Exception e) {
                log.error("Error al obtener la clave privada: {}", e.getMessage());
                return false;
            }

            log.info("✅ Certificado validado correctamente");
            return true;

        } catch (Exception e) {
            log.error("Error al validar certificado: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public LocalDate extraerFechaVencimiento(MultipartFile certificadoFile, String pin) {
        try {
            X509Certificate cert = extraerInformacionCertificado(certificadoFile, pin);
            if (cert != null) {
                return cert.getNotAfter()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            }
        } catch (Exception e) {
            log.error("Error al extraer fecha de vencimiento: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public X509Certificate extraerInformacionCertificado(MultipartFile certificadoFile, String pin) {
        try {
            String pinLimpio = pin != null ? pin.trim() : "";

            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            byte[] certificadoBytes = certificadoFile.getBytes();
            keyStore.load(new ByteArrayInputStream(certificadoBytes), pinLimpio.toCharArray());

            Enumeration<String> aliases = keyStore.aliases();
            if (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                return (X509Certificate) keyStore.getCertificate(alias);
            }
        } catch (Exception e) {
            log.error("Error al extraer información del certificado: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public byte[] encriptar(byte[] data) {
        try {
            // Asegurar que la clave tenga 32 bytes para AES-256
            byte[] keyBytes = ajustarClave(masterKey.getBytes());
            Key key = new SecretKeySpec(keyBytes, ENCRYPTION_ALGORITHM);

            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            return cipher.doFinal(data);
        } catch (Exception e) {
            log.error("Error al encriptar: {}", e.getMessage());
            throw new RuntimeException("Error al encriptar datos", e);
        }
    }

    @Override
    public byte[] desencriptar(byte[] encryptedData) {
        try {
            byte[] keyBytes = ajustarClave(masterKey.getBytes());
            Key key = new SecretKeySpec(keyBytes, ENCRYPTION_ALGORITHM);

            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);

            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            log.error("Error al desencriptar: {}", e.getMessage());
            throw new RuntimeException("Error al desencriptar datos", e);
        }
    }

    @Override
    public String sanitizarNombreComercial(String nombreComercial) {
        if (nombreComercial == null) {
            return "sin_nombre";
        }

        // Normalizar (quitar acentos)
        String normalizado = Normalizer.normalize(nombreComercial, Normalizer.Form.NFD)
            .replaceAll("[^\\p{ASCII}]", "");

        // Reemplazar caracteres especiales y espacios
        return normalizado
            .toLowerCase()
            .replaceAll("[^a-z0-9]", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_|_$", "");
    }

    /**
     * Ajusta la clave a 32 bytes para AES-256
     */
    private byte[] ajustarClave(byte[] clave) {
        byte[] claveAjustada = new byte[32];
        if (clave.length < 32) {
            System.arraycopy(clave, 0, claveAjustada, 0, clave.length);
            // Rellenar con ceros si es menor
        } else {
            System.arraycopy(clave, 0, claveAjustada, 0, 32);
        }
        return claveAjustada;
    }
}