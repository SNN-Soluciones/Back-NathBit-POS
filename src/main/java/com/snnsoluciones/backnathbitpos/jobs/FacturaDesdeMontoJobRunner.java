package com.snnsoluciones.backnathbitpos.jobs;

import com.snnsoluciones.backnathbitpos.dto.pago.FacturaDesdeMontoRequest;
import com.snnsoluciones.backnathbitpos.dto.pago.FacturaDesdeMontoResponse;
import com.snnsoluciones.backnathbitpos.service.FacturaDesdeMontoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Runner asíncrono para generación de facturas desde monto
 * Ejecuta el proceso en background reportando progreso al JobRegistry
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FacturaDesdeMontoJobRunner {

    private final FacturaDesdeMontoService facturaDesdeMontoService;

    /**
     * Ejecuta el job de generación de facturas de forma asíncrona
     *
     * @param jobId ID único del job
     * @param request Request con monto y medios de pago
     * @param sesionCajaId ID de la sesión de caja del cajero
     * @param terminalId ID del terminal
     * @param cajeroId ID del cajero que emite las facturas
     * @param sucursalId ID de la sucursal
     */
    @Async("invoiceGenExecutor")
    public void run(
        UUID jobId,
        FacturaDesdeMontoRequest request,
        Long sesionCajaId,
        Long terminalId,
        Long cajeroId,
        Long sucursalId
    ) {
        log.info("🚀 Iniciando job {} - Cajero: {}, Sesión: {}, Terminal: {}",
            jobId, cajeroId, sesionCajaId, terminalId);

        var info = JobRegistry.get(jobId);
        info.setStatus(JobRegistry.Status.RUNNING);
        info.setMessage("Ejecutando generación de facturas");
        info.setStartedAt(Instant.now());
        JobRegistry.update(info);

        try {
            // Ejecutar generación ticket por ticket con callbacks
            FacturaDesdeMontoResponse resp = facturaDesdeMontoService.generarTicketPorTicket(
                request,
                sesionCajaId,
                terminalId,
                cajeroId,
                sucursalId,
                // callback por cada factura emitida:
                () -> {
                    var i = JobRegistry.get(jobId);
                    i.setEmitidos(i.getEmitidos() + 1);
                    JobRegistry.update(i);
                },
                // shouldStop (cancel):
                () -> JobRegistry.isCanceled(jobId),
                // sleeper con jitter
                (Random rnd) -> {
                    long wait = 25 + rnd.nextInt(16); // 25..40s
                    TimeUnit.SECONDS.sleep(wait);
                }
            );

            log.info("✅ Job {} completado exitosamente. Total facturado: {}",
                jobId, resp.getResumen().getTotalFacturado());

            info = JobRegistry.get(jobId);
            info.setStatus(JobRegistry.Status.SUCCESS);
            info.setMessage("Completado - " + resp.getDocumentos().size() + " facturas generadas");
            info.setFinishedAt(Instant.now());
            JobRegistry.update(info);

        } catch (Exception e) {
            log.error("❌ Error en job {}: {}", jobId, e.getMessage(), e);

            info = JobRegistry.get(jobId);
            info.setStatus(JobRegistry.Status.FAILED);
            info.setLastError(e.getMessage());
            info.setMessage("Falló: " + e.getClass().getSimpleName());
            info.setFinishedAt(Instant.now());
            JobRegistry.update(info);
        }
    }
}