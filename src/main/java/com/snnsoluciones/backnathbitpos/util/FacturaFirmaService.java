package com.snnsoluciones.backnathbitpos.util;

import com.snnsoluciones.backnathbitpos.entity.EmpresaConfigHacienda;
import com.snnsoluciones.backnathbitpos.repository.EmpresaConfigHaciendaRepository;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FacturaFirmaService {

    private final StorageService storageService;
    private final EmpresaConfigHaciendaRepository configRepository;

    private static final String XADES_NS = "http://uri.etsi.org/01903/v1.3.2#";
    private static final String DS_NS = "http://www.w3.org/2000/09/xmldsig#";

    /**
     * Firma el XML con XAdES-BES (Basic Electronic Signature)
     * @param xmlPath Ruta del XML sin firmar en S3
     * @param empresaId ID de la empresa
     * @return byte array del XML firmado
     */
    public byte[] firmarXML(String xmlPath, Long empresaId) throws IOException {
        log.info("Firmando XML con XAdES: {}", xmlPath);

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

            // 4. Cargar certificado y clave privada
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(certificadoBytes),
                config.getPinCertificado().toCharArray());

            String alias = keyStore.aliases().nextElement();
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias,
                config.getPinCertificado().toCharArray());
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
            Certificate[] certChain = keyStore.getCertificateChain(alias);

            // 5. Parsear documento XML
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(xmlBytes));

            // 6. Crear firma XAdES
            XMLSignatureFactory sigFactory = XMLSignatureFactory.getInstance("DOM");

            String signatureId = "xmldsig-" + UUID.randomUUID().toString();

            // Crear referencias
            List<Reference> references = new ArrayList<>();

            // Primera referencia: al documento completo
            List<Transform> transforms = new ArrayList<>();
            transforms.add(sigFactory.newTransform(Transform.ENVELOPED,
                (TransformParameterSpec) null));

            Reference docRef = sigFactory.newReference("",
                sigFactory.newDigestMethod("http://www.w3.org/2001/04/xmlenc#sha256", null),
                transforms, null, signatureId + "-ref0");
            references.add(docRef);

            // Crear objeto XAdES QualifyingProperties
            Element qualifyingProperties = createXAdESProperties(doc, cert, sigFactory);
            String signedPropsId = signatureId + "-signedprops";

            // Segunda referencia: a las SignedProperties
            List<Transform> xadesTransforms = new ArrayList<>();
            xadesTransforms.add(sigFactory.newTransform(
                "http://www.w3.org/TR/2001/REC-xml-c14n-20010315",
                (TransformParameterSpec) null));

            Reference xadesRef = sigFactory.newReference(
                "#" + signedPropsId,
                sigFactory.newDigestMethod("http://www.w3.org/2001/04/xmlenc#sha256", null),
                xadesTransforms,
                "http://uri.etsi.org/01903#SignedProperties",
                null
            );
            references.add(xadesRef);

            // Crear SignedInfo con ambas referencias
            SignedInfo signedInfo = sigFactory.newSignedInfo(
                sigFactory.newCanonicalizationMethod("http://www.w3.org/TR/2001/REC-xml-c14n-20010315",
                    (C14NMethodParameterSpec) null),
                sigFactory.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null),
                references
            );

            // Crear KeyInfo con datos adicionales del certificado
            KeyInfoFactory kif = sigFactory.getKeyInfoFactory();
            List<Object> x509Content = new ArrayList<>();
            x509Content.add(cert);

            // Agregar IssuerSerial (requerido por Hacienda)
            x509Content.add(kif.newX509IssuerSerial(
                cert.getIssuerDN().getName(),
                cert.getSerialNumber()
            ));

            // Agregar SubjectName
            List<String> subjectNames = Collections.singletonList(cert.getSubjectDN().getName());
            x509Content.add(subjectNames);

            X509Data x509Data = kif.newX509Data(x509Content);
            KeyInfo keyInfo = kif.newKeyInfo(Collections.singletonList(x509Data));

            // Crear XMLObject para XAdES
            XMLObject xadesObject = sigFactory.newXMLObject(
                Collections.singletonList(new DOMStructure(qualifyingProperties)),
                null, null, null
            );

            // Crear XMLSignature con el ID correcto
            XMLSignature signature = sigFactory.newXMLSignature(signedInfo, keyInfo,
                Collections.singletonList(xadesObject),
                signatureId, null);

            // Encontrar dónde insertar la firma
            Element root = doc.getDocumentElement();
            DOMSignContext signContext = new DOMSignContext(privateKey, root);

            // Firmar el documento
            signature.sign(signContext);

            // 7. Convertir documento firmado a bytes con codificación UTF-8
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty("encoding", "UTF-8");
            transformer.setOutputProperty("indent", "no");
            transformer.transform(new DOMSource(doc), new StreamResult(baos));

            byte[] signedXmlBytes = baos.toByteArray();
            log.info("XML firmado exitosamente, tamaño: {} bytes", signedXmlBytes.length);

            return signedXmlBytes;

        } catch (Exception e) {
            log.error("Error firmando XML: {}", e.getMessage(), e);
            throw new IOException("Error en proceso de firma XAdES", e);
        }
    }

    /**
     * Crea las propiedades XAdES necesarias para la firma de Hacienda Costa Rica
     */
    private Element createXAdESProperties(Document doc, X509Certificate cert,
        XMLSignatureFactory sigFactory) throws Exception {

        String signatureId = "xmldsig-" + UUID.randomUUID().toString();

        // Crear elemento QualifyingProperties
        Element qualifyingProperties = doc.createElementNS(XADES_NS, "xades:QualifyingProperties");
        qualifyingProperties.setAttribute("xmlns:xades141", "http://uri.etsi.org/01903/v1.4.1#");
        qualifyingProperties.setAttribute("Target", "#" + signatureId);

        // SignedProperties
        Element signedProperties = doc.createElementNS(XADES_NS, "xades:SignedProperties");
        signedProperties.setAttribute("Id", signatureId + "-signedprops");
        qualifyingProperties.appendChild(signedProperties);

        // SignedSignatureProperties
        Element signedSignatureProperties = doc.createElementNS(XADES_NS, "xades:SignedSignatureProperties");
        signedProperties.appendChild(signedSignatureProperties);

        // SigningTime - formato ISO con timezone
        Element signingTime = doc.createElementNS(XADES_NS, "xades:SigningTime");
        // Formato específico que usa Hacienda: yyyy-MM-dd'T'HH:mm:ss.SSSXXX
        signingTime.setTextContent(ZonedDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")));
        signedSignatureProperties.appendChild(signingTime);

        // SigningCertificate
        Element signingCertificate = doc.createElementNS(XADES_NS, "xades:SigningCertificate");
        signedSignatureProperties.appendChild(signingCertificate);

        // Cert
        Element certElement = doc.createElementNS(XADES_NS, "xades:Cert");
        signingCertificate.appendChild(certElement);

        // CertDigest
        Element certDigest = doc.createElementNS(XADES_NS, "xades:CertDigest");
        certElement.appendChild(certDigest);

        // DigestMethod para el certificado
        Element digestMethod = doc.createElementNS(DS_NS, "ds:DigestMethod");
        digestMethod.setAttribute("Algorithm", "http://www.w3.org/2001/04/xmlenc#sha256");
        certDigest.appendChild(digestMethod);

        // DigestValue del certificado
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] certHash = md.digest(cert.getEncoded());
        Element digestValue = doc.createElementNS(DS_NS, "ds:DigestValue");
        digestValue.setTextContent(Base64.getEncoder().encodeToString(certHash));
        certDigest.appendChild(digestValue);

        // IssuerSerial
        Element issuerSerial = doc.createElementNS(XADES_NS, "xades:IssuerSerial");
        certElement.appendChild(issuerSerial);

        // X509IssuerName
        Element issuerName = doc.createElementNS(DS_NS, "ds:X509IssuerName");
        issuerName.setTextContent(cert.getIssuerDN().getName());
        issuerSerial.appendChild(issuerName);

        // X509SerialNumber
        Element serialNumber = doc.createElementNS(DS_NS, "ds:X509SerialNumber");
        serialNumber.setTextContent(cert.getSerialNumber().toString());
        issuerSerial.appendChild(serialNumber);

        // SignaturePolicyIdentifier - Política específica de Hacienda Costa Rica
        Element signaturePolicyIdentifier = doc.createElementNS(XADES_NS, "xades:SignaturePolicyIdentifier");
        signedSignatureProperties.appendChild(signaturePolicyIdentifier);

        Element signaturePolicyId = doc.createElementNS(XADES_NS, "xades:SignaturePolicyId");
        signaturePolicyIdentifier.appendChild(signaturePolicyId);

        Element sigPolicyId = doc.createElementNS(XADES_NS, "xades:SigPolicyId");
        signaturePolicyId.appendChild(sigPolicyId);

        Element identifier = doc.createElementNS(XADES_NS, "xades:Identifier");
        // URL de la política de Hacienda v4.3 (verificar si cambió para v4.4)
        identifier.setTextContent("https://www.hacienda.go.cr/ATV/ComprobanteElectronico/docs/esquemas/2016/v4.3/Resolución_General_sobre_disposiciones_técnicas_comprobantes_electrónicos_para_efectos_tributarios.pdf");
        sigPolicyId.appendChild(identifier);

        Element sigPolicyHash = doc.createElementNS(XADES_NS, "xades:SigPolicyHash");
        signaturePolicyId.appendChild(sigPolicyHash);

        Element policyDigestMethod = doc.createElementNS(DS_NS, "ds:DigestMethod");
        policyDigestMethod.setAttribute("Algorithm", "http://www.w3.org/2001/04/xmlenc#sha256");
        sigPolicyHash.appendChild(policyDigestMethod);

        Element policyDigestValue = doc.createElementNS(DS_NS, "ds:DigestValue");
        // Hash de la política de Hacienda (este valor es fijo para v4.3)
        policyDigestValue.setTextContent("nVCrSwvK8dNnGHwfLYAzNAQMe5FCQjMl2Pf+Nfu5IBw=");
        sigPolicyHash.appendChild(policyDigestValue);

        return qualifyingProperties;
    }

    /**
     * Método auxiliar para calcular el hash de un elemento
     */
    private String calculateHash(Element element, String algorithm) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty("omit-xml-declaration", "yes");
        transformer.transform(new DOMSource(element), new StreamResult(baos));

        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] hash = md.digest(baos.toByteArray());
        return Base64.getEncoder().encodeToString(hash);
    }
}