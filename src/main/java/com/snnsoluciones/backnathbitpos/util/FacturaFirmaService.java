package com.snnsoluciones.backnathbitpos.util;

import com.snnsoluciones.backnathbitpos.entity.EmpresaConfigHacienda;
import com.snnsoluciones.backnathbitpos.repository.EmpresaConfigHaciendaRepository;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.KSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import eu.europa.esig.dss.xades.signature.XAdESService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FacturaFirmaService {
    
    private final StorageService storageService;
    private final EmpresaConfigHaciendaRepository configRepository;
    
    /**
     * Firma el XML con XAdES-EPES
     */
    public String firmarXML(String xmlPath, Long empresaId) throws IOException {
        log.info("Firmando XML: {}", xmlPath);

        try {
            // 1. Obtener configuración y certificado
            EmpresaConfigHacienda config = configRepository.findByEmpresaId(empresaId)
                .orElseThrow(() -> new RuntimeException("Config Hacienda no encontrada"));

            // 2. Descargar certificado de S3
            byte[] certificadoBytes;
            try (InputStream certInputStream = storageService.downloadFile(config.getUrlCertificadoKey())) {
                certificadoBytes = certInputStream.readAllBytes();
            }

            // 3. Descargar XML a firmar
            byte[] xmlBytes;
            try (InputStream xmlInputStream = storageService.downloadFile(xmlPath)) {
                xmlBytes = xmlInputStream.readAllBytes();
            }

            // 4. Configurar firma XAdES
            DSSDocument documentToSign = new InMemoryDocument(xmlBytes, "factura.xml");

            // Crear token de firma desde el certificado P12
            try (Pkcs12SignatureToken signingToken = new Pkcs12SignatureToken(
                new ByteArrayInputStream(certificadoBytes),
                new KeyStore.PasswordProtection(config.getPinCertificado().toCharArray()))) {

                // Obtener la clave privada
                DSSPrivateKeyEntry privateKeyEntry = signingToken.getKeys().get(0);

                // Configurar parámetros de firma
                XAdESSignatureParameters parameters = new XAdESSignatureParameters();
                parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_B);
                parameters.setSignaturePackaging(SignaturePackaging.ENVELOPED);
                parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);
                parameters.setSigningCertificate(privateKeyEntry.getCertificate());
                parameters.setCertificateChain(privateKeyEntry.getCertificateChain());

                // Crear servicio de firma
                CommonCertificateVerifier verifier = new CommonCertificateVerifier();
                XAdESService service = new XAdESService(verifier);

                // Obtener datos a firmar
                ToBeSigned dataToSign = service.getDataToSign(documentToSign, parameters);

                // Firmar
                DigestAlgorithm digestAlgorithm = parameters.getDigestAlgorithm();
                SignatureValue signatureValue = signingToken.sign(dataToSign, digestAlgorithm, privateKeyEntry);

                // Adjuntar firma al documento
                DSSDocument signedDocument = service.signDocument(documentToSign, parameters, signatureValue);

                // 5. Guardar XML firmado
                String signedPath = xmlPath.replace("unsigned", "signed");
                storageService.uploadFile(
                    signedDocument.openStream(),
                    signedPath,
                    "application/xml",
                    (int) Files.size(Paths.get(signedDocument.getName()))
                );

                log.info("XML firmado guardado en: {}", signedPath);
                return signedPath;
            }

        } catch (Exception e) {
            log.error("Error firmando XML: {}", e.getMessage(), e);
            throw new IOException("Error en proceso de firma", e);
        }
    }
}