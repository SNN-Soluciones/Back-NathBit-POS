// FacturaDesdeMontoJobRunner.java
package com.snnsoluciones.backnathbitpos.jobs;

import com.snnsoluciones.backnathbitpos.dto.pago.FacturaDesdeMontoRequest;
import com.snnsoluciones.backnathbitpos.dto.pago.FacturaDesdeMontoResponse;
import com.snnsoluciones.backnathbitpos.service.FacturaDesdeMontoService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class FacturaDesdeMontoJobRunner {

    private final FacturaDesdeMontoService facturaDesdeMontoService;

    @Async("invoiceGenExecutor")
    public void run(UUID jobId, FacturaDesdeMontoRequest request) {
        var info = JobRegistry.get(jobId);
        info.setStatus(JobRegistry.Status.RUNNING);
        info.setMessage("Ejecutando");
        info.setStartedAt(Instant.now());
        JobRegistry.update(info);

        try {
            // Podés dejar tu método tal cual, pero para reportar progreso por ticket:
            // usamos una envoltura que llama generarTicketPorTicket(...)
            FacturaDesdeMontoResponse resp = facturaDesdeMontoService.generarTicketPorTicket(
                    request,
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

            info = JobRegistry.get(jobId);
            info.setStatus(JobRegistry.Status.SUCCESS);
            info.setMessage("Completado");
            info.setFinishedAt(Instant.now());
            JobRegistry.update(info);

        } catch (Exception e) {
            info = JobRegistry.get(jobId);
            info.setStatus(JobRegistry.Status.FAILED);
            info.setLastError(e.getMessage());
            info.setMessage("Falló: " + e.getClass().getSimpleName());
            info.setFinishedAt(Instant.now());
            JobRegistry.update(info);
        }
    }
}