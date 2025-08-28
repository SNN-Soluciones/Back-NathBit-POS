package com.snnsoluciones.backnathbitpos.scheduler;

import com.snnsoluciones.backnathbitpos.service.impl.FacturaElectronicaService;
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

    private final FacturaElectronicaService facturaElectronicaService;

    /**
     * Procesa facturas pendientes cada 60 segundos
     *
     * fixedDelay asegura que espera 60 segundos DESPUÉS de que termine
     * la ejecución anterior (evita overlapping)
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void procesarFacturasPendientes() {
        log.debug("Iniciando job de procesamiento de facturas electrónicas...");

        try {
            facturaElectronicaService.procesarFacturasPendientes();
        } catch (Exception e) {
            log.error("Error en job de facturas electrónicas: {}", e.getMessage(), e);
            // El job no debe fallar, continúa en el siguiente ciclo
        }
    }

    /**
     * Job de limpieza - ejecuta cada día a las 2 AM
     * Limpia procesos stuck o registros antiguos
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void limpiezaDiaria() {
        log.info("Ejecutando limpieza diaria de bitácora...");

        try {
            // Detectar y resetear procesos stuck (más de 1 hora procesando)
            facturaElectronicaService.resetearProcesosColgados();

            // Limpiar registros antiguos (más de 90 días)
            int eliminados = facturaElectronicaService.limpiarRegistrosAntiguos(90);
            log.info("Registros antiguos eliminados: {}", eliminados);

        } catch (Exception e) {
            log.error("Error en limpieza diaria: {}", e.getMessage());
        }
    }

    /**
     * Job de monitoreo - cada 5 minutos verifica salud del sistema
     * Útil para alertas tempranas
     */
    @Scheduled(fixedRate = 300000)
    public void monitorearSalud() {
        try {
            String estadoSalud = facturaElectronicaService.verificarSaludSistema();

            if (!"OK".equals(estadoSalud)) {
                log.warn("⚠️ ALERTA SISTEMA FACTURACIÓN: Estado = {}", estadoSalud);
                // Aquí podrías enviar notificación a soporte
            }
        } catch (Exception e) {
            log.error("Error verificando salud: {}", e.getMessage());
        }
    }
}