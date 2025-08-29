package com.snnsoluciones.backnathbitpos.config;

import com.snnsoluciones.backnathbitpos.repository.EmpresaConfigHaciendaRepository;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import com.snnsoluciones.backnathbitpos.sign.DssXadesSigner;
import com.snnsoluciones.backnathbitpos.sign.SignerService;
import com.snnsoluciones.backnathbitpos.sign.XadesClaimedRoleHelper;
import com.snnsoluciones.backnathbitpos.sign.policy.DefaultPolicyProvider;
import com.snnsoluciones.backnathbitpos.sign.policy.PolicyProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FirmaConfig {

  @Bean
  public XadesClaimedRoleHelper xadesClaimedRoleHelper() {
    return new XadesClaimedRoleHelper();
  }

  @Bean
  public PolicyProvider policyProvider(
      @Value("${firma.policy.url}") String url,
      @Value("${firma.policy.fallbackDigestBase64:}") String b64) {

    byte[] fb = (b64 == null || b64.isBlank()) ? null : java.util.Base64.getDecoder().decode(b64);
    return new DefaultPolicyProvider(url, eu.europa.esig.dss.enumerations.DigestAlgorithm.SHA256, fb);
  }

  @Bean
  public SignerService signerService(EmpresaConfigHaciendaRepository repo,
      StorageService storage,
      XadesClaimedRoleHelper roleHelper,
      PolicyProvider policyProvider) {
    return new DssXadesSigner(repo, storage, roleHelper, policyProvider);
  }
}