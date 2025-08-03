package com.snnsoluciones.backnathbitpos.scheduler;

import com.snnsoluciones.backnathbitpos.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupScheduler {

    private final TokenBlacklistRepository tokenBlacklistRepository;

    /**
     * Ejecuta limpieza de tokens expirados cada hora
     */
    @Scheduled(cron = "0 0 * * * *") // Cada hora
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            tokenBlacklistRepository.deleteExpiredTokens(now);
            log.info("Limpieza de tokens expirados completada");
        } catch (Exception e) {
            log.error("Error durante la limpieza de tokens expirados", e);
        }
    }

    /**
     * Ejecuta limpieza más profunda cada día a las 3 AM
     */
    @Scheduled(cron = "0 0 3 * * *") // 3:00 AM todos los días
    @Transactional
    public void dailyCleanup() {
        try {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            
            // Aquí podrías agregar más lógica de limpieza
            // Por ejemplo, archivar eventos de auditoría antiguos
            
            log.info("Limpieza diaria completada");
        } catch (Exception e) {
            log.error("Error durante la limpieza diaria", e);
        }
    }
}