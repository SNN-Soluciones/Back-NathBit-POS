package com.snnsoluciones.backnathbitpos.scheduler;

import com.snnsoluciones.backnathbitpos.entity.EmpresaConfigHacienda;
import com.snnsoluciones.backnathbitpos.repository.EmpresaConfigHaciendaRepository;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HexFormat;

@Slf4j
@Component // si prefieres activar solo en dev: @Profile("dev")
@RequiredArgsConstructor
public class CertProbeRunner implements ApplicationRunner {

  private final EmpresaConfigHaciendaRepository configRepo;
  private final StorageService storageService;

  @Override
  public void run(ApplicationArguments args) {
    final long empresaId = 1L;

    try {
      EmpresaConfigHacienda cfg = configRepo.findByEmpresaId(empresaId)
          .orElseThrow(() -> new IllegalStateException(
              "No hay EmpresaConfigHacienda para empresaId=" + empresaId));

      final String s3Key = cfg.getUrlCertificadoKey();
      final String pin = cfg.getPinCertificado();

      log.info("=== CertProbeRunner ===");
      log.info("EmpresaId={} | S3 key='{}' | PIN length={}", empresaId, s3Key,
          pin == null ? null : pin.length());

      byte[] p12Bytes = storageService.downloadFileAsBytes(s3Key);
      log.info("p12 size={} bytes | p12 SHA-256={}",
          p12Bytes.length, sha256Hex(p12Bytes));

      KeyStore ks = KeyStore.getInstance("PKCS12");
      ks.load(new ByteArrayInputStream(p12Bytes), toChars(pin));

      Enumeration<String> aliases = ks.aliases();
      if (!aliases.hasMoreElements()) {
        log.warn("El PKCS#12 no tiene entradas/alias.");
      }

      while (aliases.hasMoreElements()) {
        String alias = aliases.nextElement();
        boolean isKey = ks.isKeyEntry(alias);
        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

        log.info("-- Alias='{}' | isKeyEntry={} --", alias, isKey);
        if (cert == null) {
          log.warn("  (no hay certificado para este alias)");
          continue;
        }

        log.info("  Subject: {}", cert.getSubjectX500Principal());
        log.info("  Issuer : {}", cert.getIssuerX500Principal());
        log.info("  Serial(hex): {}", cert.getSerialNumber().toString(16));
        log.info("  Validez : {} .. {}", cert.getNotBefore(), cert.getNotAfter());

        String subjectSN = extractSubjectSerialNumber(cert);
        log.info("  Subject.SERIALNUMBER: {}", subjectSN);

        String issuer = cert.getIssuerX500Principal().getName();
        boolean pj = containsIgnoreCase(issuer, "PERSONA JURIDICA");
        boolean pf = containsIgnoreCase(issuer, "PERSONA FISICA");
        log.info("  Issuer flags -> PJ={} | PF={}", pj, pf);

        byte[] enc = cert.getEncoded();
        log.info("  X509 SHA-1  : {}", sha1Hex(enc));
        log.info("  X509 SHA-256: {}", sha256Hex(enc));
      }

      log.info("=== CertProbeRunner fin ===");

    } catch (Exception e) {
      log.error("CertProbeRunner error: {}", e.toString(), e);
    }
  }

  private static char[] toChars(String pin) {
    return pin == null ? new char[0] : pin.toCharArray();
  }

  private static String extractSubjectSerialNumber(X509Certificate cert) {
    String subject = cert.getSubjectX500Principal() != null
        ? cert.getSubjectX500Principal().getName() : null;
    if (subject == null) return null;
    for (String part : subject.split(",")) {
      String p = part.trim();
      if (p.regionMatches(true, 0, "SERIALNUMBER=", 0, "SERIALNUMBER=".length())) {
        return p.substring("SERIALNUMBER=".length()).trim();
      }
    }
    return null;
  }

  private static boolean containsIgnoreCase(String s, String needle) {
    return s != null && needle != null && s.toUpperCase().contains(needle.toUpperCase());
  }

  private static String sha1Hex(byte[] data) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
  }

  private static String sha256Hex(byte[] data) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
  }
}