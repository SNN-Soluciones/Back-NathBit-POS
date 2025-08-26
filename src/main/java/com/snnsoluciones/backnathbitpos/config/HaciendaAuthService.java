package com.snnsoluciones.backnathbitpos.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snnsoluciones.backnathbitpos.entity.EmpresaConfigHacienda;
import com.snnsoluciones.backnathbitpos.enums.mh.AmbienteHacienda;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class HaciendaAuthService {
    
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build();
    
    @Value("${hacienda.oauth.url.sandbox:https://idp.comprobanteselectronicos.go.cr/auth/realms/rut-stag/protocol/openid-connect/token}")
    private String oauthUrlSandbox;
    
    @Value("${hacienda.oauth.url.produccion:https://idp.comprobanteselectronicos.go.cr/auth/realms/rut/protocol/openid-connect/token}")
    private String oauthUrlProduccion;
    
    // Cache de tokens por empresa
    private final Map<Long, TokenInfo> tokenCache = new ConcurrentHashMap<>();
    
    @Data
    @Builder
    private static class TokenInfo {
        private String accessToken;
        private LocalDateTime expiresAt;
        
        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt.minusMinutes(5));
        }
    }
    
    public String obtenerToken(EmpresaConfigHacienda config) {
        Long empresaId = config.getEmpresa().getId();
        
        // Verificar cache
        TokenInfo cached = tokenCache.get(empresaId);
        if (cached != null && !cached.isExpired()) {
            return cached.getAccessToken();
        }

        log.info("Obteniendo token OAuth para empresa ID: {}", empresaId);
        log.info("Ambiente: {}", config.getAmbiente());
        log.info("Usuario: {}", config.getUsuarioHacienda());
        log.info("Clave: {}", config.getClaveHacienda());
        
        // Obtener nuevo token
        try {
            String url = config.getAmbiente() == AmbienteHacienda.SANDBOX
                ? oauthUrlSandbox : oauthUrlProduccion;
            
            RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "password")
                .add("username", config.getUsuarioHacienda())
                .add("password", config.getClaveHacienda()) // Ya viene desencriptada
                .add("client_id", "api-stag") // o "api-prod" según ambiente
                .add("scope", "")
                .build();
            
            Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Error autenticación Hacienda: " + response.code());
                }
                
                String json = response.body().string();
                JsonNode node = new ObjectMapper().readTree(json);
                
                String token = node.get("access_token").asText();
                int expiresIn = node.get("expires_in").asInt();
                
                // Guardar en cache
                tokenCache.put(empresaId, TokenInfo.builder()
                    .accessToken(token)
                    .expiresAt(LocalDateTime.now().plusSeconds(expiresIn))
                    .build());
                
                return token;
            }
        } catch (Exception e) {
            log.error("Error obteniendo token OAuth: {}", e.getMessage());
            throw new RuntimeException("No se pudo autenticar con Hacienda", e);
        }
    }
}