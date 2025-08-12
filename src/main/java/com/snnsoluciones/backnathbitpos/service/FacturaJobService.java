package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.FacturaJob;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para gestión de jobs asíncronos de facturación
 */
public interface FacturaJobService {
    
    /**
     * Crea un nuevo job para procesar una factura
     * 
     * @param facturaId ID de la factura
     * @param clave Clave de la factura
     * @return Job creado
     */
    FacturaJob crearJob(Long facturaId, String clave);
    
    /**
     * Busca jobs pendientes de procesar
     * 
     * @param limite Máximo de jobs a retornar
     * @return Lista de jobs pendientes
     */
    List<FacturaJob> obtenerJobsPendientes(int limite);
    
    /**
     * Procesa un job específico
     * 
     * @param jobId ID del job
     */
    void procesarJob(Long jobId);
    
    /**
     * Marca un job como completado
     */
    void marcarCompletado(Long jobId);
    
    /**
     * Marca un job con error y agenda reintento
     */
    void marcarError(Long jobId, String error);
    
    /**
     * Busca un job por clave de factura
     */
    Optional<FacturaJob> buscarPorClave(String clave);
}