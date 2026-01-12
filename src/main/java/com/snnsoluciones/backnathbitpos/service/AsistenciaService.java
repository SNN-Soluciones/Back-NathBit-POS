package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.asistencia.AsistenciaDTO;
import com.snnsoluciones.backnathbitpos.dto.asistencia.MarcarAsistenciaRequest;
import com.snnsoluciones.backnathbitpos.dto.asistencia.MarcarAsistenciaResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Servicio para gestión de asistencias (entradas/salidas)
 */
public interface AsistenciaService {
    
    /**
     * Marca entrada o salida de un usuario
     * 
     * @param usuarioId ID del usuario que marca
     * @param empresaId ID de la empresa
     * @param sucursalId ID de la sucursal
     * @param request Datos de la marcación (tipo, observaciones)
     * @return Response con datos de la marcación
     */
    MarcarAsistenciaResponse marcarAsistencia(
        Long usuarioId,
        Long empresaId,
        Long sucursalId,
        MarcarAsistenciaRequest request
    );
    
    /**
     * Verifica si un usuario tiene entrada activa hoy
     * 
     * @param usuarioId ID del usuario
     * @return true si tiene entrada activa
     */
    boolean tieneEntradaActiva(Long usuarioId);
    
    /**
     * Verifica si un usuario tiene entrada activa en una fecha específica
     * 
     * @param usuarioId ID del usuario
     * @param fecha Fecha a verificar
     * @return true si tiene entrada activa
     */
    boolean tieneEntradaActivaEnFecha(Long usuarioId, LocalDate fecha);
    
    /**
     * Lista asistencias de una empresa en una fecha
     * 
     * @param empresaId ID de la empresa
     * @param fecha Fecha
     * @return Lista de asistencias
     */
    List<AsistenciaDTO> listarAsistenciasPorEmpresaYFecha(Long empresaId, LocalDate fecha);
    
    /**
     * Lista usuarios presentes (con entrada activa) en una empresa
     * 
     * @param empresaId ID de la empresa
     * @param fecha Fecha (normalmente hoy)
     * @return Lista de asistencias activas
     */
    List<AsistenciaDTO> listarUsuariosPresentes(Long empresaId, LocalDate fecha);
    
    /**
     * Obtiene el historial de asistencias de un usuario
     * 
     * @param usuarioId ID del usuario
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de asistencias
     */
    List<AsistenciaDTO> obtenerHistorial(Long usuarioId, LocalDate fechaInicio, LocalDate fechaFin);
}