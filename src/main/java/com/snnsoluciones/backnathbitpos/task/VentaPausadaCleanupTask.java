package com.snnsoluciones.backnathbitpos.task;

import com.snnsoluciones.backnathbitpos.repository.VentaPausadaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class VentaPausadaCleanupTask {
    
    private final VentaPausadaRepository repository;
    
    // Ejecutar cada hora
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void limpiarVentasExpiradas() {
        log.info("Iniciando limpieza de ventas pausadas expiradas...");
        
        try {
            LocalDateTime ahora = LocalDateTime.now();
            int eliminadas = repository.deleteExpiredVentas(ahora);
            
            if (eliminadas > 0) {
                log.info("Se eliminaron {} ventas pausadas expiradas", eliminadas);
            }
        } catch (Exception e) {
            log.error("Error al limpiar ventas pausadas expiradas", e);
        }
    }
}