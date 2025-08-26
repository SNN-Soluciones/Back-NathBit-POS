package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.FacturaJob;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.enums.facturacion.PasoFacturacion;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FacturaJobService {

    // --- Lectura / selección de trabajo ---
    List<FacturaJob> obtenerJobsPendientes(int limite);
    List<FacturaJob> obtenerJobsPendientesPorPasos(Collection<PasoFacturacion> pasos, int limite);
    FacturaJob crearJob(Long facturaId, String clave);

    Optional<FacturaJob> findById(Long id);
    Optional<FacturaJob> findByClave(String clave);

    // --- Ciclo de vida del job ---
    void marcarProcesando(Long jobId);
    void marcarCompletado(Long jobId);
    void marcarCancelado(Long jobId, String motivo);

    // Error + backoff exponencial
    void marcarError(Long jobId, String error);

    // --- Avance de pasos ---
    void avanzarPaso(Long jobId, PasoFacturacion siguientePaso);
    void setPaso(Long jobId, PasoFacturacion paso);

    // --- Programación / locking suave ---
    void actualizarProximaEjecucion(Long jobId, LocalDateTime proxima);
    boolean tryClaim(Long jobId, String workerId, LocalDateTime ttl); // opcional si implementas locking

    // --- Vínculos a artefactos (para trazabilidad rápida desde el job) ---
    void setXmlUnsignedPath(Long jobId, String s3Path);
    void setXmlSignedPath(Long jobId, String s3Path);
    void setRespuestaPath(Long jobId, String s3Path);

    void reprogramarPorClave(String clave, LocalDateTime proximaEjecucion);
    void finalizarPorClave(String clave, String mensaje);
    void avanzarPasoPorClave(String clave, PasoFacturacion paso);

    List<FacturaJob> obtenerJobsPorEstadosExcluidos(List<EstadoFactura> estadosExcluidos, int limite);
}