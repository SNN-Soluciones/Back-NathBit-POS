package com.snnsoluciones.backnathbitpos.sign.policy;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;

/** Provee los datos de la política XAdES-EPES (v4.4) para DGT. */
public interface PolicyProvider {
  /** URL pública del PDF de la resolución/política. */
  String getPolicyUrl();

  /** Algoritmo de digest para la política (SHA-256 recomendado). */
  DigestAlgorithm getDigestAlgorithm();

  /**
   * Digest binario del PDF de política (exacto). Puede venir de config,
   * precalculado o descargado+cacheado.
   */
  byte[] getPolicyDigest();
}