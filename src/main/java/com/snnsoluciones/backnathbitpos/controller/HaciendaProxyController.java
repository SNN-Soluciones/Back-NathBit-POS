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
    private static final String GOMETA_API_KEY = "iyPaqeKXKCCgwKX"; // luego lo pasamos a properties

    @GetMapping("/contribuyente/{identificacion}")
    public ResponseEntity<?> getContribuyente(@PathVariable String identificacion) {

        try {
            // ---------- INTENTO HACIENDA ----------
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

        } catch (Exception ex) {
            // ---------- FALLBACK A GOMETA ----------
            return consultarGometa(identificacion, ex);
        }
    }

    /**
     * Fallback Gometa
     */
    private ResponseEntity<?> consultarGometa(String identificacion, Exception causa) {
        try {
            String url = GOMETA_API_BASE + "/cedulas/" + identificacion + "?key=" + GOMETA_API_KEY;

            ResponseEntity<Object> response =
                restTemplate.getForEntity(url, Object.class);

            return ResponseEntity.ok(
                Map.of(
                    "fuente", "gometa",
                    "data", response.getBody()
                )
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                    "error", "No fue posible consultar ni Hacienda ni Gometa",
                    "detalleHacienda", causa.getMessage(),
                    "detalleGometa", e.getMessage()
                ));
        }
    }
}