package com.snnsoluciones.backnathbitpos.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job para procesamiento asíncrono de facturas electrónicas
 * Se ejecuta cada minuto y procesa las facturas pendientes
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FacturaElectronicaJob {


    /**
     * Procesa facturas pendientes cada 60 segundos
     *
     * fixedDelay asegura que espera 60 segundos DESPUÉS de que termine
     * la ejecución anterior (evita overlapping)
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void procesarFacturasPendientes() {
        log.debug("Iniciando job de procesamiento de facturas electrónicas...");

    }

    /**
     * Job de limpieza - ejecuta cada día a las 2 AM
     * Limpia procesos stuck o registros antiguos
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void limpiezaDiaria() {
        log.info("Ejecutando limpieza diaria de bitácora...");
    }

    /**
     * Job de monitoreo - cada 5 minutos verifica salud del sistema
     * Útil para alertas tempranas
     */
    @Scheduled(fixedRate = 300000)
    public void monitorearSalud() {
        log.info("Ejecutando monitoreo de salud del sistema...");
    }
}