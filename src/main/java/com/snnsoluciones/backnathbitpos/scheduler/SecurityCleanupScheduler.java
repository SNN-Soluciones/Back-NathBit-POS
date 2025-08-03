package com.snnsoluciones.backnathbitpos.scheduler;

import com.snnsoluciones.backnathbitpos.config.security.RateLimiterConfig;
import com.snnsoluciones.backnathbitpos.repository.AuditEventRepository;
import com.snnsoluciones.backnathbitpos.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduler para tareas de limpieza y mantenimiento del módulo de seguridad.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityCleanupScheduler {

    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final AuditEventRepository auditEventRepository;
    private final RateLimiterConfig rateLimiterConfig;

    @Value("${security.cleanup.audit-retention-days:90}")
    private int auditRetentionDays;

    /**
     * Limpia tokens expirados de la blacklist.
     * Se ejecuta cada hora.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            log.info("Iniciando limpieza de tokens expirados...");
            LocalDateTime now = LocalDateTime.now();
            tokenBlacklistRepository.deleteExpiredTokens(now);
            log.info("Limpieza de tokens expirados completada");
        } catch (Exception e) {
            log.error("Error durante limpieza de tokens: {}", e.getMessage());
        }
    }

    /**
     * Limpia eventos de auditoría antiguos.
     * Se ejecuta diariamente a las 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldAuditEvents() {
        try {
            log.info("Iniciando limpieza de eventos de auditoría antiguos...");
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(auditRetentionDays);
            auditEventRepository.deleteOldEvents(cutoffDate);
            log.info("Limpieza de eventos de auditoría completada");
        } catch (Exception e) {
            log.error("Error durante limpieza de eventos de auditoría: {}", e.getMessage());
        }
    }

    /**
     * Limpia entradas antiguas del rate limiter.
     * Se ejecuta cada 30 minutos.
     */
    @Scheduled(cron = "0 */30 * * * *")
    public void cleanupRateLimiterCache() {
        try {
            log.debug("Limpiando cache del rate limiter...");
            rateLimiterConfig.cleanupOldEntries();
        } catch (Exception e) {
            log.error("Error durante limpieza del rate limiter: {}", e.getMessage());
        }
    }

    /**
     * Resetea contadores de intentos fallidos de usuarios bloqueados.
     * Se ejecuta diariamente a las 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void resetBlockedUsersCounters() {
        try {
            log.info("Reseteando contadores de usuarios bloqueados hace más de 24 horas...");
            // Esta lógica podría implementarse según las reglas de negocio
            // Por ejemplo, resetear usuarios bloqueados hace más de 24 horas
        } catch (Exception e) {
            log.error("Error reseteando contadores de usuarios: {}", e.getMessage());
        }
    }
}