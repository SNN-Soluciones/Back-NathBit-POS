package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.FacturaJob;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoProcesoJob;
import com.snnsoluciones.backnathbitpos.enums.facturacion.PasoFacturacion;
import com.snnsoluciones.backnathbitpos.repository.FacturaJobRepository;
import com.snnsoluciones.backnathbitpos.service.FacturaAsyncProcessor;
import com.snnsoluciones.backnathbitpos.service.FacturaJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FacturaJobServiceImpl implements FacturaJobService {
    
    private final FacturaJobRepository jobRepository;
    
    @Autowired
    @Lazy // Para evitar dependencia circular con AsyncProcessor
    private FacturaAsyncProcessor asyncProcessor;
    
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
    public List<FacturaJob> obtenerJobsPendientes(int limite) {
        LocalDateTime ahora = LocalDateTime.now();
        List<FacturaJob> jobs = jobRepository.findJobsPendientes(ahora)
            .stream()
            .limit(limite)
            .toList();
        
        log.debug("Se encontraron {} jobs pendientes de procesar", jobs.size());
        return jobs;
    }
    
    @Override
    public void procesarJob(Long jobId) {
        FacturaJob job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job no encontrado: " + jobId));
        
        if (!job.getEstadoProceso().puedeReintentar() && job.getEstadoProceso() != EstadoProcesoJob.PENDIENTE) {
            log.warn("Job {} no puede ser procesado en estado: {}", jobId, job.getEstadoProceso());
            return;
        }
        
        try {
            // Marcar como procesando
            job.setEstadoProceso(EstadoProcesoJob.PROCESANDO);
            jobRepository.save(job);
            
            // Delegar al procesador asíncrono
            asyncProcessor.procesarFactura(job);
            
        } catch (Exception e) {
            log.error("Error procesando job {}: {}", jobId, e.getMessage());
            marcarError(jobId, e.getMessage());
        }
    }
    
    @Override
    public void marcarCompletado(Long jobId) {
        FacturaJob job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job no encontrado: " + jobId));
        
        job.setEstadoProceso(EstadoProcesoJob.COMPLETADO);
        job.setPasoActual(PasoFacturacion.COMPLETADO);
        
        jobRepository.save(job);
        log.info("Job {} marcado como completado", jobId);
    }
    
    @Override
    public void marcarError(Long jobId, String error) {
        FacturaJob job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job no encontrado: " + jobId));
        
        job.incrementarIntentos();
        job.setUltimoError(error);
        
        // Si puede reintentarse, marcar para reintento
        if (job.puedeReintentarse()) {
            job.setEstadoProceso(EstadoProcesoJob.REINTENTANDO);
            log.warn("Job {} marcado para reintento #{} en {} minutos. Error: {}", 
                jobId, job.getIntentos(), Math.pow(2, job.getIntentos() - 1), error);
        } else {
            job.setEstadoProceso(EstadoProcesoJob.ERROR);
            log.error("Job {} alcanzó máximo de reintentos. Error final: {}", jobId, error);
        }
        
        jobRepository.save(job);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<FacturaJob> buscarPorClave(String clave) {
        return jobRepository.findByClave(clave);
    }
}