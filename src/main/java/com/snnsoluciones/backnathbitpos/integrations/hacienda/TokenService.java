package com.snnsoluciones.backnathbitpos.integrations.hacienda;

import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.HaciendaAuthParams;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.HaciendaTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final HaciendaClient haciendaClient;
    private final Map<String, CachedToken> cache = new ConcurrentHashMap<>(); // key = empresaId+env

    public String getValidToken(HaciendaAuthParams params) {
        String key = params.getEmpresaId() + ":" + (params.isSandbox() ? "SBX" : "PRD");
        CachedToken cached = cache.get(key);
        Instant now = Instant.now();

        if (cached != null && now.isBefore(cached.expiresAt.minusSeconds(30))) {
            return cached.accessToken;
        }
        HaciendaTokenResponse tr = haciendaClient.getToken(params);
        long ttl = Math.max(30, tr.getExpiresIn() - 30); // margen
        CachedToken ct = new CachedToken(tr.getAccessToken(), now.plusSeconds(ttl));
        cache.put(key, ct);
        log.debug("[TokenService] Nuevo token cacheado para {} expira en {}s", key, ttl);
        return ct.accessToken;
    }

    @Value
    static class CachedToken {
        String accessToken;
        Instant expiresAt;
    }
}
