package com.snnsoluciones.backnathbitpos.integrations.hacienda;

import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class HaciendaClient {

  private final WebClient webClient; // define un bean WebClient (ver config)

  private static final String TOKEN_URL_PROD = "https://idp.comprobanteselectronicos.go.cr/auth/realms/rut/protocol/openid-connect/token";
  private static final String TOKEN_URL_STAG = "https://idp.comprobanteselectronicos.go.cr/auth/realms/rut-stag/protocol/openid-connect/token";

  private static final String BASE_PROD = "https://api.comprobanteselectronicos.go.cr/recepcion/v1";
  private static final String BASE_STAG = "https://api.comprobanteselectronicos.go.cr/recepcion-sandbox/v1";

  @Autowired
  private RestTemplate haciendaRestTemplate;

  // ======================
  // TOKEN
  // ======================
  public HaciendaTokenResponse getToken(HaciendaAuthParams params) {
    String tokenUrl = params.isSandbox()
        ? "https://idp.comprobanteselectronicos.go.cr/auth/realms/rut-stag/protocol/openid-connect/token"
        : "https://idp.comprobanteselectronicos.go.cr/auth/realms/rut/protocol/openid-connect/token";

    log.info("[Hacienda] Obteniendo token para empresa {} (sandbox={}) user={}",
        params.getEmpresaId(), params.isSandbox(), params.getUsername());

    return webClient.post()
        .uri(tokenUrl)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(BodyInserters
            .fromFormData("grant_type", "password")
            .with("client_id", params.getClientId())            // api-stag | api-prod
            .with("username", params.getUsername())
            .with("password", params.getPassword())             // caracteres especiales ok
            .with("scope", "openid"))                           // <- importante
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .onStatus(
            HttpStatusCode::isError,
            resp -> resp.bodyToMono(String.class).flatMap(body -> {
              log.error("[Hacienda] Error {} obteniendo token. Body: {}", resp.statusCode(), body);
              return Mono.error(new RuntimeException("Token error: " + resp.statusCode()));
            })
        )
        .bodyToMono(HaciendaTokenResponse.class)
        .block();
  }

  // ======================
  // POST /recepcion
  // ======================
  public String postRecepcion(String bearerToken, boolean sandbox, RecepcionRequest body) {
    final String base = sandbox
        ? "https://api.comprobanteselectronicos.go.cr/recepcion-sandbox/v1"
        : "https://api.comprobanteselectronicos.go.cr/recepcion/v1";
    final String url = base + "/recepcion";

    // IMPORTANTÍSIMO: token CRUDO (sin el prefijo 'Bearer ')
    final String rawToken = (bearerToken == null)
        ? ""
        : bearerToken.replaceFirst("(?i)^\\s*Bearer\\s+", "").trim();

    // Headers
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    headers.setBearerAuth(rawToken); // => Authorization: Bearer <token>

    // En tu payload, asegúrate que 'fecha' tenga offset -06:00 (ISO_OFFSET_DATE_TIME)
    // Ej: "2025-08-25T20:15:12-06:00"
    HttpEntity<RecepcionRequest> entity = new HttpEntity<>(body, headers);

    try {
      ResponseEntity<Void> resp = haciendaRestTemplate.postForEntity(url, entity, Void.class);
      // Suele venir 201/202 y header Location
      String location = resp.getHeaders().getFirst(HttpHeaders.LOCATION);
      if (location == null || location.isBlank()) {
        location = base + "/recepcion/" + body.getClave();
      }
      return location;
    } catch (HttpClientErrorException e) {
      // Log útil para ver exactamente qué respondió MH
      e.getResponseBodyAsString();
      String bodyErr = e.getResponseBodyAsString();
      throw new IllegalStateException("Error en /recepcion: status=" + e.getStatusCode()
          + " body=" + bodyErr, e);
    }
  }


  // ======================
  // GET /recepcion/{clave}
  // ======================
  public ConsultaEstadoResponse getEstado(String bearerToken, boolean sandbox, String clave) {
    final String url = (sandbox
        ? "https://api.comprobanteselectronicos.go.cr/recepcion-sandbox/v1/recepcion/"
        : "https://api.comprobanteselectronicos.go.cr/recepcion/v1/recepcion/") + clave;

    final String rawToken = (bearerToken == null)
        ? ""
        : bearerToken.replaceFirst("(?i)^\\s*Bearer\\s+", "").trim();

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    headers.setBearerAuth(rawToken);

    try {
      ResponseEntity<ConsultaEstadoResponse> resp = haciendaRestTemplate.exchange(
          url, HttpMethod.GET, new HttpEntity<Void>(headers), ConsultaEstadoResponse.class);

      return resp.getBody();
    } catch (HttpClientErrorException e) {
      e.getResponseBodyAsString();
      String bodyErr = e.getResponseBodyAsString();
      throw new IllegalStateException("Error en GET estado: status=" + e.getStatusCode()
          + " body=" + bodyErr, e);
    }
  }

  public static class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String m) {
      super(m);
    }
  }
}
