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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
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
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            keyStore.load(new ByteArrayInputStream(certificadoFile.getBytes()), 
                         pin.toCharArray());
            
            // Verificar que el keystore tenga al menos una entrada
            Enumeration<String> aliases = keyStore.aliases();
            if (!aliases.hasMoreElements()) {
                log.error("El certificado no contiene entradas válidas");
                return false;
            }
            
            // Verificar que podemos leer el certificado
            String alias = aliases.nextElement();
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
            
            // Verificar que no esté expirado
            cert.checkValidity();
            
            log.info("Certificado validado correctamente para: {}", 
                     cert.getSubjectDN().getName());
            return true;
            
        } catch (Exception e) {
            log.error("Error al validar certificado: {}", e.getMessage());
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
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            keyStore.load(new ByteArrayInputStream(certificadoFile.getBytes()), 
                         pin.toCharArray());
            
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
            Key key = new SecretKeySpec(ajustarLlaveMaestra(), ENCRYPTION_ALGORITHM);
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (Exception e) {
            log.error("Error al encriptar datos: {}", e.getMessage());
            throw new RuntimeException("Error al encriptar certificado", e);
        }
    }
    
    @Override
    public byte[] desencriptar(byte[] encryptedData) {
        try {
            Key key = new SecretKeySpec(ajustarLlaveMaestra(), ENCRYPTION_ALGORITHM);
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            log.error("Error al desencriptar datos: {}", e.getMessage());
            throw new RuntimeException("Error al desencriptar certificado", e);
        }
    }
    
    @Override
    public String sanitizarNombreComercial(String nombreComercial) {
        if (nombreComercial == null) {
            return "sin_nombre";
        }
        
        return nombreComercial
            .toLowerCase()
            .trim()
            .replaceAll("[^a-z0-9]+", "_")  // Reemplazar caracteres especiales por _
            .replaceAll("_+", "_")          // Eliminar _ múltiples
            .replaceAll("^_|_$", "");       // Eliminar _ al inicio o final
    }
    
    /**
     * Ajusta la llave maestra a 16, 24 o 32 bytes para AES
     */
    private byte[] ajustarLlaveMaestra() {
        // Asegurar que la llave tenga 32 bytes (256 bits) para AES-256
        byte[] keyBytes = masterKey.getBytes();
        byte[] adjustedKey = new byte[32];
        
        // Copiar bytes de la llave original
        System.arraycopy(keyBytes, 0, adjustedKey, 0, 
                        Math.min(keyBytes.length, adjustedKey.length));
        
        // Si la llave es más corta, rellenar con un patrón
        for (int i = keyBytes.length; i < adjustedKey.length; i++) {
            adjustedKey[i] = (byte) (i % 256);
        }
        
        return adjustedKey;
    }
}