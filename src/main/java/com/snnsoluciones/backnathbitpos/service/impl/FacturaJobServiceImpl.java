package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.FacturaJob;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoProcesoJob;
import com.snnsoluciones.backnathbitpos.enums.facturacion.PasoFacturacion;
import com.snnsoluciones.backnathbitpos.repository.FacturaJobRepository;
import com.snnsoluciones.backnathbitpos.repository.FacturaRepository;
import com.snnsoluciones.backnathbitpos.service.FacturaJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacturaJobServiceImpl implements FacturaJobService {

    private static final int MAX_INTENTOS = 5;
    private static final int BACKOFF_MINUTES_CAP = 30; // cap duro para no dispararse

    private final FacturaJobRepository jobRepository;
    private final FacturaRepository facturaRepository;

    // -------------------- Lectura / selección --------------------

    @Override
    @Transactional(readOnly = true)
    public List<FacturaJob> obtenerJobsPendientes(int limite) {
        return jobRepository.findJobsPendientes(PageRequest.of(0, limite));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacturaJob> obtenerJobsPendientesPorPasos(Collection<PasoFacturacion> pasos, int limite) {
        // Si no tienes query dedicada, filtramos en memoria los pendientes por paso
        Set<PasoFacturacion> filtro = Set.copyOf(pasos);
        return jobRepository.findJobsPendientesPorPasos(pasos)
                .stream()
                .filter(j -> filtro.contains(j.getPasoActual()))
                .limit(limite)
                .toList();
    }

    @Override
    public FacturaJob crearJob(Long facturaId, String clave) {
        log.info("Creando job para factura: {} con clave: {}", facturaId, clave);

        // Verificar si ya existe un job para esta factura
        if (jobRepository.existsByFacturaId(facturaId)) {
            log.warn("Ya existe un job para la factura: {}", facturaId);
            return jobRepository.findByClave(clave).orElseThrow();
        }

        FacturaJob job = new FacturaJob();
        job.setFacturaId(facturaId);
        job.setClave(clave);
        job.setEstadoProceso(EstadoProcesoJob.PENDIENTE);
        job.setPasoActual(PasoFacturacion.GENERAR_XML);
        job.setProximaEjecucion(LocalDateTime.now());
        job.setIntentos(0);

        FacturaJob jobGuardado = jobRepository.save(job);
        log.info("Job creado con ID: {} para clave: {}", jobGuardado.getId(), clave);

        return jobGuardado;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FacturaJob> findById(Long id) {
        return jobRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FacturaJob> findByClave(String clave) {
        return jobRepository.findByClave(clave);
    }

    // -------------------- Ciclo de vida --------------------

    @Override
    @Transactional
    public void marcarProcesando(Long jobId) {
        FacturaJob job = require(jobId);
        job.setEstadoProceso(EstadoProcesoJob.PROCESANDO);
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    @Override
    @Transactional
    public void marcarCompletado(Long jobId) {
        FacturaJob job = require(jobId);
        job.setEstadoProceso(EstadoProcesoJob.COMPLETADO);
        job.setPasoActual(PasoFacturacion.COMPLETADO);
        job.setProximaEjecucion(null);
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    @Override
    @Transactional
    public void marcarCancelado(Long jobId, String motivo) {
        FacturaJob job = require(jobId);
        job.setEstadoProceso(EstadoProcesoJob.CANCELADO);
        job.setUltimoError(motivo);
        job.setProximaEjecucion(null);
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    @Override
    @Transactional
    public void marcarError(Long jobId, String error) {
        FacturaJob job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job no encontrado: " + jobId));

        job.incrementarIntentos(); // encapsula el ++ y updatedAt

        job.setUltimoError(error);

        final boolean reintenta = job.puedeReintentarse();
        if (reintenta) {
            job.setEstadoProceso(EstadoProcesoJob.REINTENTANDO);
            // backoff: 1, 2, 4, 8, 16 min...
            long minutes = (long) Math.pow(2, Math.max(0, job.getIntentos() - 1));
            job.setProximaEjecucion(LocalDateTime.now().plusMinutes(minutes));
            log.warn("Job {} marcado para reintento #{} en {} min. Error: {}",
                jobId, job.getIntentos(), minutes, error);
        } else {
            job.setEstadoProceso(EstadoProcesoJob.ERROR);
            // IMPORTANTE: columna NOT NULL → nunca dejarla en null
            job.setProximaEjecucion(LocalDateTime.now());
            log.error("Job {} alcanzó máximo de reintentos. Error final: {}", jobId, error);
        }

        // Limpia claim si aplica
        job.setClaimedAt(null);
        job.setClaimedBy(null);

        jobRepository.save(job);
    }

    // -------------------- Avance de pasos --------------------

    @Override
    @Transactional
    public void avanzarPaso(Long jobId, PasoFacturacion siguientePaso) {
        FacturaJob job = require(jobId);

        job.setPasoActual(siguientePaso);
        job.setEstadoProceso(EstadoProcesoJob.PENDIENTE);   // o el que uses para “ok”
        job.setUltimoError(null);                             // limpia error previo
        job.setIntentos(0);                                   // reset de reintentos
        job.setProximaEjecucion(LocalDateTime.now());         // NOT NULL garantizado
        job.setClaimedAt(null);                               // libera claim
        job.setClaimedBy(null);
        job.setUpdatedAt(LocalDateTime.now());

        jobRepository.save(job);
    }

    @Override
    @Transactional
    public void setPaso(Long jobId, PasoFacturacion paso) {
        FacturaJob job = require(jobId);
        job.setPasoActual(paso);
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    // -------------------- Scheduling / locking simple --------------------

    @Override
    @Transactional
    public void actualizarProximaEjecucion(Long jobId, LocalDateTime proxima) {
        FacturaJob job = require(jobId);
        job.setProximaEjecucion(proxima);
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    @Override
    @Transactional
    public boolean tryClaim(Long jobId, String workerId, LocalDateTime ttl) {
        // Implementación simple (no atómica). Para verdadero locking, usa una columna worker_id + query @Modifying.
        FacturaJob job = require(jobId);
        if (job.getEstadoProceso() == EstadoProcesoJob.PENDIENTE || job.getEstadoProceso() == EstadoProcesoJob.REINTENTANDO) {
            job.setEstadoProceso(EstadoProcesoJob.PROCESANDO);
            job.setProximaEjecucion(ttl);
            job.setUpdatedAt(LocalDateTime.now());
            jobRepository.save(job);
            return true;
        }
        return false;
    }

    // -------------------- Rutas de artefactos (traza) --------------------

    @Override
    @Transactional
    public void setXmlUnsignedPath(Long jobId, String s3Path) {
        FacturaJob job = require(jobId);
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    @Override
    @Transactional
    public void setXmlSignedPath(Long jobId, String s3Path) {
        FacturaJob job = require(jobId);
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    @Override
    @Transactional
    public void setRespuestaPath(Long jobId, String s3Path) {
        // Si luego agregas campo específico en FacturaJob, setéalo aquí.
        FacturaJob job = require(jobId);
        // job.setRespuestaPath(s3Path);
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    @Override
    @Transactional
    public void avanzarPasoPorClave(String clave, PasoFacturacion paso) {
        facturaRepository.findByClave(clave).ifPresent(factura -> {
            FacturaJob job = jobRepository.findByFacturaId(factura.getId()).orElse(null);
            if (job != null) {
                job.setPasoActual(paso);
                job.setProximaEjecucion(LocalDateTime.now());
                jobRepository.save(job);
            }
        });
    }

    @Override
    @Transactional
    public void finalizarPorClave(String clave, String mensaje) {
        facturaRepository.findByClave(clave).ifPresent(factura -> {
            FacturaJob job = jobRepository.findByFacturaId(factura.getId()).orElse(null);
            if (job != null) {
                job.setUltimoError(mensaje);
                job.setEstadoProceso(EstadoProcesoJob.COMPLETADO);
                job.setProximaEjecucion(null);
                jobRepository.save(job);
            }
        });
    }

    @Override
    @Transactional
    public void reprogramarPorClave(String clave, LocalDateTime proximaEjecucion) {
        facturaRepository.findByClave(clave).ifPresent(factura -> {
            FacturaJob job = jobRepository.findByFacturaId(factura.getId()).orElse(null);
            if (job != null) {
                job.setProximaEjecucion(proximaEjecucion);
                jobRepository.save(job);
            }
        });
    }

    // -------------------- Helper --------------------
    private FacturaJob require(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job no encontrado: " + id));
    }
}