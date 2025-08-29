package com.snnsoluciones.backnathbitpos.sign;

import com.snnsoluciones.backnathbitpos.entity.EmpresaConfigHacienda;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import com.snnsoluciones.backnathbitpos.repository.EmpresaConfigHaciendaRepository;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.MimeType;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.Policy;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import eu.europa.esig.dss.xades.signature.XAdESService;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Calendar;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class DssXadesSigner implements SignerService {

  private final EmpresaConfigHaciendaRepository configRepo;
  private final StorageService storage;
  private final XadesClaimedRoleHelper roleHelper;
  private final com.snnsoluciones.backnathbitpos.sign.policy.PolicyProvider policyProvider;

  @Override
  public byte[] signXmlForEmpresa(byte[] xmlUnsigned, Long empresaId, TipoDocumento tipo) {
    long t0 = System.currentTimeMillis();
    try {
      EmpresaConfigHacienda cfg = configRepo.findByEmpresaId(empresaId)
          .orElseThrow(() -> new IllegalStateException(
              "Config Hacienda no encontrada para empresa " + empresaId));

      String certPath = cfg.getUrlCertificadoKey(); // S3 key
      if (certPath == null || certPath.isBlank()) {
        throw new IllegalStateException("Empresa sin certificado (.p12) configurado");
      }

      byte[] p12Bytes = storage.downloadFileAsBytes(certPath);
      char[] pin = safePin(cfg.getPinCertificado());

      // Documento a firmar
      InMemoryDocument toSign = new InMemoryDocument(
          xmlUnsigned,
          "comprobante.xml",
          MimeType.fromMimeTypeString("application/xml")
      );
      KeyStore.PasswordProtection pp = new KeyStore.PasswordProtection(pin);

      try (Pkcs12SignatureToken token =
          new Pkcs12SignatureToken(new ByteArrayInputStream(p12Bytes), pp)) {

        List<DSSPrivateKeyEntry> keys = token.getKeys(); // o token.getKey("alias") si quisieras
        if (keys == null || keys.isEmpty()) {
          throw new IllegalStateException("El .p12 no contiene llaves privadas");
        }
        DSSPrivateKeyEntry entry = keys.get(0);

        log.info("Firmando: empresaId={} | s3Key='{}'", empresaId, certPath);
        log.info("PIN length={}", pin.length);

        log.info("p12 SHA-256={}", HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(p12Bytes)));

        X509Certificate cert = (X509Certificate) entry.getCertificate().getCertificate();
        log.info("X509 SHA-256={}", HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(cert.getEncoded())));
        log.info("Subject={}", cert.getSubjectX500Principal());
        log.info("Issuer={}", cert.getIssuerX500Principal());
        log.info("Cert sujeto: {}", cert.getSubjectX500Principal());
        log.info("Cert emisor: {}", cert.getIssuerX500Principal());
        log.info("Cert serial: {}", cert.getSerialNumber().toString(16));
        log.info("Cert válido: {} .. {}", cert.getNotBefore(), cert.getNotAfter());

// Validar PF vs PJ y (opcional) cédula
        String tipoIdent = cfg.getEmpresa().getTipoIdentificacion()
            .getCodigo(); // "01"/"02"/"03"/"04"
        String identificacion = cfg.getEmpresa().getIdentificacion();
        assertCertMatchesEmpresa(cert, tipoIdent, identificacion);

        // Parámetros XAdES
        XAdESSignatureParameters params = new XAdESSignatureParameters();
        params.setSignatureLevel(
            SignatureLevel.XAdES_BASELINE_B); // EPES se expresa con policy B-Level + Policy
        params.setSignaturePackaging(SignaturePackaging.ENVELOPED);
        params.setDigestAlgorithm(DigestAlgorithm.SHA256);
        params.setSigningCertificate(entry.getCertificate());
        params.setCertificateChain(entry.getCertificateChain());
        params.bLevel().setSigningDate(Calendar.getInstance().getTime());

        // Política (EPES)
        Policy pol = new Policy();
        pol.setId(policyProvider.getPolicyUrl());
        if (policyProvider.getPolicyDigest() != null) {
          pol.setDigestAlgorithm(policyProvider.getDigestAlgorithm());
          pol.setDigestValue(policyProvider.getPolicyDigest());
        }
        params.bLevel().setSignaturePolicy(pol);

        // ClaimedRole (solo MR -> "Receptor")
        String claimedRole = roleHelper.resolveClaimedRole(tipo);
        if (claimedRole != null && !claimedRole.isBlank()) {
          params.bLevel().setClaimedSignerRoles(List.of(claimedRole));
        }

        // Service + sign
        CommonCertificateVerifier verifier = new CommonCertificateVerifier();
        XAdESService service = new XAdESService(verifier);

        ToBeSigned tbs = service.getDataToSign(toSign,
            params);            // respeta transforms/c14n
        SignatureValue sig = token.sign(tbs, params.getDigestAlgorithm(), entry);
        DSSDocument signed = service.signDocument(toSign, params, sig);

        byte[] out = signed.openStream().readAllBytes(); // NO reprocesar/pretty-print
        long ms = System.currentTimeMillis() - t0;
        log.info("XML firmado OK (empresaId={}, tipo={}, ms={})", empresaId, tipo, ms);
        return out;
      }
    } catch (Exception e) {
      log.error("Fallo firmando XML (empresaId={}, tipo={}, ts={}): {}", empresaId, tipo,
          Instant.now(), e.toString());
      throw wrap(e);
    }
  }

  private RuntimeException wrap(Exception e) {
    String msg = e.getMessage();
    if (msg != null) {
      if (msg.contains("keystore password was incorrect") || msg.toLowerCase()
          .contains("password")) {
        return new RuntimeException("PIN del certificado inválido.");
      }
      if (msg.toLowerCase().contains("malformed") || msg.toLowerCase().contains("xml")) {
        return new RuntimeException("XML inválido o malformado.");
      }
      if (e instanceof GeneralSecurityException) {
        return new RuntimeException("Certificado/llave no válidos.");
      }
    }
    return new RuntimeException("Error firmando XML", e);
  }

  private char[] safePin(String pin) {
    if (pin == null || pin.isBlank()) {
      throw new IllegalStateException("PIN del certificado vacío");
    }
    return pin.toCharArray();
  }

  private static boolean isIssuerPersonaJuridica(X509Certificate cert) {
    String issuer = cert.getIssuerX500Principal().getName();
    return issuer != null && issuer.toUpperCase().contains("PERSONA JURIDICA");
  }

  private static boolean isIssuerPersonaFisica(X509Certificate cert) {
    String issuer = cert.getIssuerX500Principal().getName();
    return issuer != null && issuer.toUpperCase().contains("PERSONA FISICA");
  }

  private static String extractSubjectSerialNumber(X509Certificate cert) {
    // OID 2.5.4.5 = SERIALNUMBER (suele contener la cédula)
    // Lo buscamos en el DN textual para no meternos a parsear ASN.1 a mano.
    String subject = cert.getSubjectX500Principal().getName(); // formato RFC2253
    if (subject == null) {
      return null;
    }
    // Busca SERIALNUMBER=XXXX
    for (String part : subject.split(",")) {
      String p = part.trim();
      if (p.toUpperCase().startsWith("SERIALNUMBER=")) {
        return p.substring("SERIALNUMBER=".length()).trim();
      }
    }
    return null;
  }

  private static String onlyDigits(String s) {
    return s == null ? null : s.replaceAll("\\D+", "");
  }

  private static void assertCertMatchesEmpresa(X509Certificate cert, String tipoIdentCodigo,
      String identificacionEmpresa) {
    boolean pj = isIssuerPersonaJuridica(cert);
    boolean pf = isIssuerPersonaFisica(cert);

    // Map simple: 01=física, 02=jurídica
    if ("01".equals(tipoIdentCodigo)) {
      if (!pf) {
        throw new IllegalStateException(
            "El certificado no corresponde a PERSONA FÍSICA (empresa tipo 01). Emisor del cert: ["
                + cert.getIssuerX500Principal() + "]");
      }
    } else if ("02".equals(tipoIdentCodigo)) {
      if (!pj) {
        throw new IllegalStateException(
            "El certificado no corresponde a PERSONA JURÍDICA (empresa tipo 02). Emisor del cert: ["
                + cert.getIssuerX500Principal() + "]");
      }
    } else {
      // 03 DIMEX, 04 NITE → no forzamos, pero avisamos en log
      if (!pj && !pf) {
        // algunos emisores no incluyen “PERSONA FISICA/JURIDICA” literal
        // solo INFO
      }
    }

    // (Opcional recomendado) validar identificación contra el Subject
    String subjSerial = extractSubjectSerialNumber(cert);
    String cedulaCert = onlyDigits(subjSerial);
    String cedulaEmp = onlyDigits(identificacionEmpresa);

    if (cedulaCert != null && cedulaEmp != null && !cedulaEmp.isBlank()) {
      if (!cedulaEmp.equals(cedulaCert)) {
        throw new IllegalStateException("La cédula del certificado (" + cedulaCert +
            ") no coincide con la de la empresa (" + cedulaEmp + ").");
      }
    }
  }
}