package com.snnsoluciones.backnathbitpos.integrations.hacienda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.ConsultaEstadoResponse;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.HaciendaAuthParams;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.HaciendaTokenResponse;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.RecepcionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Implementación de cliente para la API de Hacienda (Recepción v1).
 * - POST /recepcion  (devuelve Location)
 * - GET  /recepcion/{clave}
 * - getToken(...) (si ya tienes tu flujo de token, puedes mantenerlo o usar este)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HaciendaClient {

  private final RestTemplateBuilder restTemplateBuilder;
  private final ObjectMapper objectMapper;

  // Bases API
  private static final String BASE_PROD    = "https://api.comprobanteselectronicos.go.cr/recepcion/v1";
  private static final String BASE_SANDBOX = "https://api.comprobanteselectronicos.go.cr/recepcion-sandbox/v1";

  private RestTemplate restTemplate() {
    return restTemplateBuilder
        .connectTimeout(Duration.ofSeconds(20))
        .readTimeout(Duration.ofSeconds(60))
        .build();
  }

  private String base(boolean produccion) {
    return produccion ? BASE_PROD : BASE_SANDBOX;
  }

  // ===================== TOKEN =====================
  // Mantén tu implementación si ya tienes un IDP distinto o flujo custom.
  // Este método es solo un ejemplo genérico (x-www-form-urlencoded).

  public HaciendaTokenResponse getToken(HaciendaAuthParams params) {
    if (params == null) throw new IllegalArgumentException("Params token nulos");

    // IMPORTANTE: Ajusta estos endpoints según tu IdP real
    // (Keycloak/IdP de MH o el que uses). Aquí va un placeholder:
    String tokenEndpoint = params.isSandbox()
        ? "https://idp-sandbox.comprobanteselectronicos.go.cr/auth/realms/rut/protocol/openid-connect/token"
        : "https://idp.comprobanteselectronicos.go.cr/auth/realms/rut/protocol/openid-connect/token";

    RestTemplate rt = restTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.setAcceptCharset(java.util.List.of(StandardCharsets.UTF_8));

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "password");
    form.add("client_id", params.getClientId());
    form.add("username", params.getUsername());
    form.add("password", params.getPassword());

    HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(form, headers);

    try {
      ResponseEntity<HaciendaTokenResponse> resp =
          rt.postForEntity(tokenEndpoint, req, HaciendaTokenResponse.class);
      if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
        return resp.getBody();
      }
      throw new IllegalStateException("Token no obtenido. Status=" + resp.getStatusCode());
    } catch (HttpClientErrorException | HttpServerErrorException e) {
      log.error("Error getToken: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
      throw e;
    }
  }

  // ===================== POST /recepcion =====================
  public ResponseEntity<Void> postRecepcion(String accessToken, boolean produccion, RecepcionRequest payload) {
    if (accessToken == null || accessToken.isBlank()) throw new IllegalArgumentException("accessToken vacío");
    if (payload == null) throw new IllegalArgumentException("payload nulo");

    String url = base(produccion) + "/recepcion";
    RestTemplate rt = restTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
    headers.setBearerAuth(accessToken);

    HttpEntity<RecepcionRequest> entity = new HttpEntity<>(payload, headers);

    try {
      ResponseEntity<Void> resp = rt.postForEntity(url, entity, Void.class);
      // 201 Created esperado; header "Location" puede traer la ruta de consulta
      if (resp.getStatusCode() == HttpStatus.CREATED) {
        String loc = resp.getHeaders().getFirst(HttpHeaders.LOCATION);
        log.info("POST /recepcion OK. Location={}", loc);
      } else {
        log.warn("POST /recepcion Status inesperado: {}", resp.getStatusCode());
      }
      return resp;
    } catch (HttpClientErrorException | HttpServerErrorException e) {
      log.error("Error POST /recepcion: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
      throw e;
    }
  }

  // ===================== GET /recepcion/{clave} =====================
  public ConsultaEstadoResponse getEstado(String accessToken, boolean sandbox, String clave) {
    if (accessToken == null || accessToken.isBlank()) throw new IllegalArgumentException("accessToken vacío");
    if (clave == null || clave.isBlank()) throw new IllegalArgumentException("clave vacía");

    String url = (sandbox ? BASE_SANDBOX : BASE_PROD) + "/recepcion/" + clave;
    RestTemplate rt = restTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
    headers.setBearerAuth(accessToken);

    HttpEntity<Void> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<ConsultaEstadoResponse> resp =
          rt.exchange(url, HttpMethod.GET, entity, ConsultaEstadoResponse.class);
      if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
        // Nota: algunos IDP/ambientes devuelven "ind-estado" como "procesando"/"aceptado"/"rechazado"
        return resp.getBody();
      }
      throw new IllegalStateException("GET /recepcion sin body. Status=" + resp.getStatusCode());
    } catch (HttpClientErrorException | HttpServerErrorException e) {
      log.error("Error GET /recepcion/{}: status={} body={}", clave, e.getStatusCode(), e.getResponseBodyAsString());
      throw e;
    }
  }

  public ResponseEntity<Void> postMensajeReceptor(String accessToken, boolean produccion, RecepcionRequest payload) {
    return postRecepcion(accessToken, produccion, payload);
  }

  // ===================== Helper opcional =====================
  // Útil para loggear JSONs de request/response cuando depuras (sin romper tipado)
  private String toJson(Object o) {
    try { return objectMapper.writeValueAsString(o); }
    catch (Exception e) { return String.valueOf(o); }
  }
}