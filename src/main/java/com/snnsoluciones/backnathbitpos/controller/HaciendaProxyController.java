package com.snnsoluciones.backnathbitpos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/proxy/hacienda")
public class HaciendaProxyController {

    private static final String HACIENDA_API_BASE = "https://api.hacienda.go.cr";
    private static final String GOMETA_API_BASE = "https://apis.gometa.org";

    @Autowired
    private RestTemplate restTemplate;

    @Value("${gometa.api-key:}")
    private String gometaApiKey;

    /**
     * Endpoint único: usa Hacienda por defecto y hace fallback a GoMeta en 429/5xx/timeout.
     */
    @GetMapping("/contribuyente/{identificacion}")
    public ResponseEntity<?> getContribuyente(@PathVariable String identificacion) {
        // (Opcional) Sanitizar un poco la identificación
        String id = identificacion.trim();

        // 1) Intento con Hacienda
        try {
            Map<String, Object> body = callHacienda(id);
            return ResponseEntity.ok(normalizeResponse("hacienda", body));
        } catch (RestClientResponseException e) {
            // Errores con status HTTP
            if (shouldFallback(e.getRawStatusCode())) {
                // 2) Fallback a GoMeta
                return tryGoMetaFallback(id, e);
            }
            return ResponseEntity.status(e.getRawStatusCode())
                .body(error("Error Hacienda: " + safeMsg(e), e.getRawStatusCode()));
        } catch (ResourceAccessException e) {
            // Timeouts / conexión -> intentar GoMeta
            return tryGoMetaFallback(id, e);
        } catch (Exception e) {
            // Cualquier otro error no esperado -> intentar GoMeta
            return tryGoMetaFallback(id, e);
        }
    }

    /* ===================== Helpers ===================== */

    private Map<String, Object> callHacienda(String identificacion) {
        String url = HACIENDA_API_BASE + "/fe/ae?identificacion=" + identificacion;

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "NathBit-POS/1.0");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        return response.getBody() != null ? response.getBody() : new HashMap<>();
    }

    private Map<String, Object> callGoMeta(String identificacion) {
        if (gometaApiKey == null || gometaApiKey.isBlank()) {
            throw new IllegalStateException("Falta configurar 'gometa.api-key' en application.properties");
        }
        // Doc de GoMeta usa patrón /cedulas/{id}?key=...
        String url = GOMETA_API_BASE + "/cedulas/" + identificacion + "?key=" + gometaApiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "NathBit-POS/1.0");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        return response.getBody() != null ? response.getBody() : new HashMap<>();
    }

    private boolean shouldFallback(int statusCode) {
        // 429 Too Many Requests o 5xx
        return statusCode == 429 || (statusCode >= 500 && statusCode <= 599);
    }

    private ResponseEntity<?> tryGoMetaFallback(String identificacion, Exception originalEx) {
        try {
            Map<String, Object> body = callGoMeta(identificacion);
            return ResponseEntity.ok(normalizeResponse("gometa", body));
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getRawStatusCode())
                .body(error("Error GoMeta: " + safeMsg(e), e.getRawStatusCode()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(error("Ambos proveedores fallaron. Hacienda: " + safeMsg(originalEx)
                        + " | GoMeta: " + safeMsg(e),
                    HttpStatus.BAD_GATEWAY.value()));
        }
    }

    private Map<String, Object> normalizeResponse(String source, Map<String, Object> data) {
        Map<String, Object> out = new HashMap<>();
        out.put("source", source);
        out.put("data", data);
        return out;
    }

    private Map<String, Object> error(String message, int status) {
        Map<String, Object> out = new HashMap<>();
        out.put("error", message);
        out.put("status", status);
        return out;
    }

    private String safeMsg(Exception e) {
        String m = e.getMessage();
        return (m == null) ? e.getClass().getSimpleName() : m;
    }
}