package com.snnsoluciones.backnathbitpos.config.security;

import com.snnsoluciones.backnathbitpos.service.audit.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Componente para limitar intentos de login y prevenir ataques de fuerza bruta.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimiterConfig {

    @Value("${security.rate-limit.max-attempts:5}")
    private int maxAttempts;

    @Value("${security.rate-limit.block-duration-minutes:15}")
    private int blockDurationMinutes;

    private final AuditService auditService;

    // Cache en memoria para tracking de intentos
    // En producción, esto debería estar en Redis para soportar múltiples instancias
    private final Map<String, LoginAttemptInfo> attemptsCache = new ConcurrentHashMap<>();

    /**
     * Registra un intento de login exitoso.
     */
    public void loginSucceeded(String key) {
        attemptsCache.remove(key);
        log.debug("Login exitoso para: {}", key);
    }

    /**
     * Registra un intento de login fallido.
     */
    public void loginFailed(String key, HttpServletRequest request) {
        LoginAttemptInfo attemptInfo = attemptsCache.computeIfAbsent(key, k -> new LoginAttemptInfo());
        attemptInfo.incrementAttempts();
        attemptInfo.setLastAttemptTime(LocalDateTime.now());
        
        log.warn("Intento de login fallido #{} para: {}", attemptInfo.getAttempts(), key);
        
        if (isBlocked(key)) {
            String details = String.format("IP/Usuario bloqueado tras %d intentos fallidos. Bloqueado hasta: %s", 
                maxAttempts, attemptInfo.getBlockedUntil());
            auditService.logEvent("LOGIN_BLOCKED", details, false, "Demasiados intentos fallidos");
        }
    }

    /**
     * Verifica si una clave (IP o usuario) está bloqueada.
     */
    public boolean isBlocked(String key) {
        LoginAttemptInfo attemptInfo = attemptsCache.get(key);
        if (attemptInfo == null) {
            return false;
        }

        // Si ya estaba bloqueado y el tiempo no ha expirado
        if (attemptInfo.getBlockedUntil() != null && 
            LocalDateTime.now().isBefore(attemptInfo.getBlockedUntil())) {
            return true;
        }

        // Si excedió el máximo de intentos, bloquear
        if (attemptInfo.getAttempts() >= maxAttempts) {
            attemptInfo.setBlockedUntil(LocalDateTime.now().plusMinutes(blockDurationMinutes));
            log.error("Bloqueando {} por {} minutos tras {} intentos fallidos", 
                key, blockDurationMinutes, attemptInfo.getAttempts());
            return true;
        }

        return false;
    }

    /**
     * Obtiene el número de intentos restantes antes del bloqueo.
     */
    public int getRemainingAttempts(String key) {
        LoginAttemptInfo attemptInfo = attemptsCache.get(key);
        if (attemptInfo == null) {
            return maxAttempts;
        }
        return Math.max(0, maxAttempts - attemptInfo.getAttempts());
    }

    /**
     * Obtiene el tiempo restante de bloqueo en minutos.
     */
    public long getBlockedMinutesRemaining(String key) {
        LoginAttemptInfo attemptInfo = attemptsCache.get(key);
        if (attemptInfo == null || attemptInfo.getBlockedUntil() == null) {
            return 0;
        }

        long minutesRemaining = TimeUnit.SECONDS.toMinutes(
            java.time.Duration.between(LocalDateTime.now(), attemptInfo.getBlockedUntil()).getSeconds()
        );
        
        return Math.max(0, minutesRemaining);
    }

    /**
     * Limpia entradas antiguas del cache (método para ser llamado por un scheduler).
     */
    public void cleanupOldEntries() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        attemptsCache.entrySet().removeIf(entry -> {
            LoginAttemptInfo info = entry.getValue();
            return info.getLastAttemptTime().isBefore(cutoffTime) ||
                   (info.getBlockedUntil() != null && info.getBlockedUntil().isBefore(LocalDateTime.now()));
        });
    }

    /**
     * Genera una clave compuesta para el rate limiting.
     * Combina IP + username para mayor seguridad.
     */
    public String generateKey(String username, String ipAddress) {
        return String.format("%s_%s", ipAddress, username);
    }

    /**
     * Clase interna para almacenar información de intentos de login.
     */
    private static class LoginAttemptInfo {
        private int attempts = 0;
        private LocalDateTime lastAttemptTime = LocalDateTime.now();
        private LocalDateTime blockedUntil;

        public void incrementAttempts() {
            this.attempts++;
        }

        // Getters y setters
        public int getAttempts() {
            return attempts;
        }

        public LocalDateTime getLastAttemptTime() {
            return lastAttemptTime;
        }

        public void setLastAttemptTime(LocalDateTime lastAttemptTime) {
            this.lastAttemptTime = lastAttemptTime;
        }

        public LocalDateTime getBlockedUntil() {
            return blockedUntil;
        }

        public void setBlockedUntil(LocalDateTime blockedUntil) {
            this.blockedUntil = blockedUntil;
        }
    }
}