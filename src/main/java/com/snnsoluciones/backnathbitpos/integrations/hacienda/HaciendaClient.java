package com.snnsoluciones.backnathbitpos.integrations.hacienda;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snnsoluciones.backnathbitpos.entity.EmpresaConfigHacienda;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.ConsultaEstadoResponse;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.HaciendaAuthParams;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.HaciendaTokenResponse;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.IdentificacionDTO;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.RecepcionRequest;
import com.snnsoluciones.backnathbitpos.repository.EmpresaConfigHaciendaRepository;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
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
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

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
  private final EmpresaConfigHaciendaRepository empresaConfigHaciendaRepository;
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

  /**
   * Enviar mensaje receptor a Hacienda
   * @param empresaId ID de la empresa
   * @param xmlMensajeReceptor XML del mensaje receptor firmado
   * @return Respuesta de Hacienda
   */
  /**
   * Enviar mensaje receptor a Hacienda
   */
  public String enviarMensajeReceptor(Long empresaId,
      String xmlMensajeReceptor,
      IdentificacionDTO receptorDelComprobante) {

    final boolean PRODUCCION = true;

    try {
      // 1) Config empresa
      EmpresaConfigHacienda config = empresaConfigHaciendaRepository
          .findByEmpresaId(empresaId)
          .orElseThrow(() -> new RuntimeException("No hay config MH para empresa: " + empresaId));

      HaciendaAuthParams authParams = HaciendaAuthParams.builder()
          .username(config.getUsuarioHacienda())
          .password(config.getClaveHacienda())
          .clientId("api-prod")
          .sandbox(!PRODUCCION)
          .build();

      // 2) Token
      HaciendaTokenResponse tokenResponse = getToken(authParams);
      String accessToken = tokenResponse.getAccessToken();

      // 3) Fecha y clave
      String fechaFormateada = ZonedDateTime.now(ZoneId.of("America/Costa_Rica"))
          .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
      String clave = extraerClaveDeXml(xmlMensajeReceptor);

      // 4) Payload POST /recepcion
      String base64Xml = Base64.getEncoder().encodeToString(
          xmlMensajeReceptor.getBytes(StandardCharsets.UTF_8));

      RecepcionRequest payload = RecepcionRequest.builder()
          .clave(clave)
          .fecha(fechaFormateada)
          .emisor(receptorDelComprobante) // EMISOR = proveedor del comprobante original
          .receptor(IdentificacionDTO.builder() // RECEPTOR = tu empresa
              .tipoIdentificacion(config.getEmpresa().getTipoIdentificacion().getCodigo())
              .numeroIdentificacion(config.getEmpresa().getIdentificacion())
              .build())
          .comprobanteXml(base64Xml)
          .build();

      // 5) POST /recepcion
      ResponseEntity<Void> response = postMensajeReceptor(accessToken, PRODUCCION, payload);
      if (response.getStatusCode().is2xxSuccessful()) {
        return "Mensaje receptor enviado exitosamente";
      }
      // Si no fue 2xx, cae abajo al catch para manejo homogéneo
      throw new RuntimeException("Error enviando MR: " + response.getStatusCode());

    } catch (HttpClientErrorException e) {
      // Manejo idempotente: si es 400, consultamos estado por clave
      if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
        try {
          // Extrae clave de nuevo (del XML original)
          String clave = extraerClaveDeXml(xmlMensajeReceptor);

          // 1er intento con token actual
          ConsultaEstadoResponse est = null;
          try {
            est = getEstado(/*accessToken=*/getToken(HaciendaAuthParams.builder()
                    .username(empresaConfigHaciendaRepository.findByEmpresaId(empresaId)
                        .orElseThrow().getUsuarioHacienda())
                    .password(empresaConfigHaciendaRepository.findByEmpresaId(empresaId)
                        .orElseThrow().getClaveHacienda())
                    .clientId("api-prod")
                    .sandbox(false)
                    .build()).getAccessToken(),
                /*sandbox=*/false, clave);
          } catch (HttpClientErrorException ex1) {
            if (ex1.getStatusCode() == HttpStatus.BAD_REQUEST) {
              // Refresca token y reintenta 1 vez
              HaciendaTokenResponse nuevo = getToken(HaciendaAuthParams.builder()
                  .username(empresaConfigHaciendaRepository.findByEmpresaId(empresaId)
                      .orElseThrow().getUsuarioHacienda())
                  .password(empresaConfigHaciendaRepository.findByEmpresaId(empresaId)
                      .orElseThrow().getClaveHacienda())
                  .clientId("api-prod")
                  .sandbox(false)
                  .build());
              est = getEstado(nuevo.getAccessToken(), false, clave);
            } else {
              throw ex1;
            }
          }

          if (est != null && est.getIndEstado() != null) {
            String ind = est.getIndEstado().toLowerCase();

            // Si ya está aceptado/recibido/procesando en MH, devolvemos respuesta y “seguimos”
            if (ind.equals("aceptado") || ind.equals("recibido") || ind.equals("procesando")) {
              // Si viene respuesta-xml (base64), devuélvela para que el caller la suba a S3
              if (est.getRespuestaXmlBase64() != null && !est.getRespuestaXmlBase64().isBlank()) {
                String xmlResp = new String(Base64.getDecoder()
                    .decode(est.getRespuestaXmlBase64()), StandardCharsets.UTF_8);
                return xmlResp; // el service la tratará como XML de respuesta MH
              }
              // Si no hay XML, retorna marcador de estado
              return "MH-ESTADO:" + ind;
            }

            // Si está rechazado, falla explícitamente
            if (ind.equals("rechazado")) {
              throw new RuntimeException("MR rechazado por MH (consulta estado)");
            }
          }

          // Si no logramos determinar estado, relanza el error original
          throw new RuntimeException("POST 400 y no se pudo confirmar estado en MH: "
              + e.getResponseBodyAsString(), e);

        } catch (Exception q) {
          throw new RuntimeException("POST 400 y GET estado falló: " + q.getMessage(), q);
        }
      }

      // Otros 4xx: relanzar con detalle
      throw new RuntimeException("Error HTTP " + e.getStatusCode() + ": "
          + e.getResponseBodyAsString(), e);

    } catch (HttpServerErrorException e) {
      throw new RuntimeException("Error servidor Hacienda: " + e.getResponseBodyAsString(), e);

    } catch (Exception e) {
      throw new RuntimeException("Error enviando mensaje receptor: " + e.getMessage(), e);
    }
  }

  /**
   * Extraer clave numérica del XML
   */
  private String extraerClaveDeXml(String xml) {
    try {
      // Parsear XML para extraer la clave
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(new InputSource(new StringReader(xml)));

      NodeList claveNodes = doc.getElementsByTagName("Clave");
      if (claveNodes.getLength() > 0) {
        return claveNodes.item(0).getTextContent();
      }

      throw new RuntimeException("No se encontró la clave en el XML");
    } catch (Exception e) {
      throw new RuntimeException("Error extrayendo clave del XML: " + e.getMessage(), e);
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