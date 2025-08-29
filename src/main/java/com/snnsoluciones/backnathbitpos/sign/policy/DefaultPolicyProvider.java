package com.snnsoluciones.backnathbitpos.sign.policy;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.Objects;

public class DefaultPolicyProvider implements PolicyProvider {

  private final String policyUrl;
  private final DigestAlgorithm algo;
  private final byte[] fallbackDigest; // opcional (puede ser null)
  private volatile byte[] cachedDigest;

  private final OkHttpClient http = new OkHttpClient.Builder()
      .callTimeout(Duration.ofSeconds(20))
      .build();

  public DefaultPolicyProvider(String policyUrl, DigestAlgorithm algo, byte[] fallbackDigest) {
    this.policyUrl = Objects.requireNonNull(policyUrl);
    this.algo = algo == null ? DigestAlgorithm.SHA256 : algo;
    this.fallbackDigest = fallbackDigest;
  }

  @Override public String getPolicyUrl() { return policyUrl; }
  @Override public DigestAlgorithm getDigestAlgorithm() { return algo; }

  @Override
  public byte[] getPolicyDigest() {
    if (cachedDigest != null) return cachedDigest;
    try {
      Request req = new Request.Builder().url(policyUrl).get().build();
      try (Response resp = http.newCall(req).execute()) {
        if (!resp.isSuccessful() || resp.body() == null) throw new RuntimeException("HTTP " + resp.code());
        byte[] pdf = resp.body().bytes();
        MessageDigest md = MessageDigest.getInstance(algo.getName());
        cachedDigest = md.digest(pdf);
        return cachedDigest;
      }
    } catch (Exception e) {
      if (fallbackDigest != null) return fallbackDigest;
      throw new RuntimeException("No se pudo obtener digest de la política", e);
    }
  }
}