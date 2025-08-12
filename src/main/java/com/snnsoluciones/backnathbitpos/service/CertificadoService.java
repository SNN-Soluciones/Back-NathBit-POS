package com.snnsoluciones.backnathbitpos.service;

import org.springframework.web.multipart.MultipartFile;
import java.security.cert.X509Certificate;
import java.time.LocalDate;

public interface CertificadoService {
    
    /**
     * Valida un certificado P12 con su PIN
     * @param certificadoFile archivo del certificado
     * @param pin PIN del certificado
     * @return true si es válido
     */
    boolean validarCertificado(MultipartFile certificadoFile, String pin);
    
    /**
     * Extrae la fecha de vencimiento del certificado
     * @param certificadoFile archivo del certificado
     * @param pin PIN del certificado
     * @return fecha de vencimiento
     */
    LocalDate extraerFechaVencimiento(MultipartFile certificadoFile, String pin);
    
    /**
     * Extrae información del certificado
     * @param certificadoFile archivo del certificado
     * @param pin PIN del certificado
     * @return certificado X509
     */
    X509Certificate extraerInformacionCertificado(MultipartFile certificadoFile, String pin);
    
    /**
     * Encripta datos usando AES-256
     * @param data datos a encriptar
     * @return datos encriptados
     */
    byte[] encriptar(byte[] data);
    
    /**
     * Desencripta datos usando AES-256
     * @param encryptedData datos encriptados
     * @return datos desencriptados
     */
    byte[] desencriptar(byte[] encryptedData);
    
    /**
     * Sanitiza el nombre comercial para usar como carpeta
     * @param nombreComercial nombre comercial de la empresa
     * @return nombre sanitizado
     */
    String sanitizarNombreComercial(String nombreComercial);
}