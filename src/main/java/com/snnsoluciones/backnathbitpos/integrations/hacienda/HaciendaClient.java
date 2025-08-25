package com.snnsoluciones.backnathbitpos.integrations.hacienda;

import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
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

    // ======================
    // TOKEN
    // ======================
    public HaciendaTokenResponse getToken(HaciendaAuthParams params) {
        String tokenUrl = params.isSandbox() ? TOKEN_URL_STAG : TOKEN_URL_PROD;

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", params.getClientId()); // api-stag | api-prod
        form.add("username", params.getUsername());
        form.add("password", params.getPassword());

        return webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(HaciendaTokenResponse.class)
                .block();
    }

    // ======================
    // POST /recepcion
    // ======================
    public String postRecepcion(String bearerToken, boolean sandbox, RecepcionRequest body) {
        String base = sandbox ? BASE_STAG : BASE_PROD;
        String url = base + "/recepcion";

        ClientResponse resp = webClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .block();

        if (resp == null) throw new IllegalStateException("No response from Hacienda /recepcion");

        if (resp.statusCode().is2xxSuccessful() || resp.statusCode() == HttpStatus.CREATED) {
            // Hacienda suele devolver 201 y Location con la URL de consulta
            String location = resp.headers().asHttpHeaders().getFirst(HttpHeaders.LOCATION);
            log.info("[Hacienda] /recepcion OK (status={} location={})", resp.statusCode(), location);
            return location;
        }

        String xCause = resp.headers().asHttpHeaders().getFirst("X-Error-Cause");
        String bodyErr = resp.bodyToMono(String.class).blockOptional().orElse("");
        log.warn("[Hacienda] /recepcion FAIL status={} cause={} body={}", resp.statusCode(), xCause, bodyErr);

        if (resp.statusCode() == HttpStatus.BAD_REQUEST && xCause != null && xCause.toLowerCase().contains("recibido")) {
            // Ya recibido: tratamos como OK idempotente
            return base + "/recepcion/" + body.getClave();
        }
        if (resp.statusCode() == HttpStatus.UNAUTHORIZED) {
            throw new UnauthorizedException("401 en /recepcion: token inválido o expirado");
        }
        throw new IllegalStateException("Error en /recepcion: status=" + resp.statusCode() + " cause=" + xCause);
    }

    // ======================
    // GET /recepcion/{clave}
    // ======================
    public ConsultaEstadoResponse getEstado(String bearerToken, boolean sandbox, String clave) {
        String base = sandbox ? BASE_STAG : BASE_PROD;
        String url = base + "/recepcion/" + clave;

        return webClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "bearer " + bearerToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(ConsultaEstadoResponse.class)
                .onErrorResume(ex -> {
                    log.error("[Hacienda] Error consultando estado {}: {}", clave, ex.getMessage());
                    return Mono.error(ex);
                })
                .block();
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String m) { super(m); }
    }
}
