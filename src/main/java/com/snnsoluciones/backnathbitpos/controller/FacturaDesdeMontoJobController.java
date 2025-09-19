// FacturaDesdeMontoJobController.java
package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.pago.FacturaDesdeMontoRequest;
import com.snnsoluciones.backnathbitpos.jobs.FacturaDesdeMontoJobRunner;
import com.snnsoluciones.backnathbitpos.jobs.JobRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/tickets/jobs")
@RequiredArgsConstructor
public class FacturaDesdeMontoJobController {

    private final FacturaDesdeMontoJobRunner runner;

    @PostMapping
    public Map<String, Object> crearJob(@RequestBody FacturaDesdeMontoRequest request) {
        UUID jobId = JobRegistry.newJob();
        runner.run(jobId, request); // asíncrono
        return Map.of("jobId", jobId.toString(), "status", "EN_QUEUE");
    }

    @GetMapping("/{jobId}")
    public Object estado(@PathVariable String jobId) {
        var info = JobRegistry.get(UUID.fromString(jobId));
        return (info != null) ? info : Map.of("error", "job no encontrado");
    }

    @PostMapping("/{jobId}/cancel")
    public Map<String, Object> cancelar(@PathVariable String jobId) {
        UUID id = UUID.fromString(jobId);
        JobRegistry.cancel(id);
        return Map.of("jobId", jobId, "status", "CANCEL_REQUESTED");
    }
}