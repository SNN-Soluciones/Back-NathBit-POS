package com.snnsoluciones.backnathbitpos.dto.pago;

import com.snnsoluciones.backnathbitpos.service.CuentaPorCobrarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CuentaPorCobrarScheduler {
    
    private final CuentaPorCobrarService cuentaPorCobrarService;
    
    /**
     * Actualizar estados de cuentas vencidas
     * Se ejecuta todos los días a las 12:05 AM
     */
    @Scheduled(cron = "0 5 0 * * *")
    public void actualizarEstadosVencidos() {
        log.info("=== INICIANDO PROCESO AUTOMÁTICO DE ACTUALIZACIÓN DE CUENTAS ===");
        try {
            cuentaPorCobrarService.actualizarEstadosVencidos();
        } catch (Exception e) {
            log.error("Error en proceso automático de cuentas: ", e);
        }
    }
    
    /**
     * Actualizar estados cada 4 horas durante el día (8am, 12pm, 4pm, 8pm)
     * Por si hay cambios durante el día
     */
    @Scheduled(cron = "0 0 8,12,16,20 * * *")
    public void actualizarEstadosDiurnos() {
        log.info("=== ACTUALIZACIÓN DIURNA DE ESTADOS ===");
        try {
            cuentaPorCobrarService.actualizarEstadosVencidos();
        } catch (Exception e) {
            log.error("Error en actualización diurna: ", e);
        }
    }
}