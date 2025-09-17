// HaciendaProxyController.java
package com.snnsoluciones.backnathbitpos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import java.util.Map;

@RestController
@RequestMapping("/api/proxy/hacienda")
public class HaciendaProxyController {

    @Autowired
    private RestTemplate restTemplate;

    private static final String HACIENDA_API_BASE = "https://api.hacienda.go.cr";
    private static final String GOMETA_API_BASE = "https://apis.gometa.org";

    /**
     * Proxy para consultar información de contribuyente en Hacienda
     */
    @GetMapping("/contribuyente/{identificacion}")
    public ResponseEntity<?> getContribuyente(@PathVariable String identificacion) {
        try {
            String url = HACIENDA_API_BASE + "/fe/ae?identificacion=" + identificacion;

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "NathBit-POS/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Object.class
            );

            return ResponseEntity.ok(response.getBody());

        } catch (HttpClientErrorException e) {
            // Manejar errores específicos como 429 (Too Many Requests)
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                        "error", "Demasiadas solicitudes. Por favor intente más tarde.",
                        "status", 429
                    ));
            }
            return ResponseEntity.status(e.getStatusCode())
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al consultar Hacienda: " + e.getMessage()));
        }
    }

    /**
     * Proxy para consultar información en Gometa (API alternativo)
     */
    @GetMapping("/gometa/{identificacion}")
    public ResponseEntity<?> getContribuyenteGometa(@PathVariable String identificacion) {
        try {
            // Necesitas obtener la API key de Gometa
            String apiKey = "iyPaqeKXKCCgwKX";
            String url = GOMETA_API_BASE + "/cedulas/" + identificacion + "&key=" + apiKey;

            ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al consultar Gometa: " + e.getMessage()));
        }
    }
}