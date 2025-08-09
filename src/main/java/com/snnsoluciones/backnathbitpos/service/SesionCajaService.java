package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.SesionCaja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SesionCajaService {
    
    // Gestión de sesiones
    SesionCaja abrirSesion(Long usuarioId, Long terminalId, BigDecimal montoInicial);
    SesionCaja cerrarSesion(Long sesionId, BigDecimal montoCierre, String observaciones);
    Optional<SesionCaja> buscarSesionActiva(Long usuarioId);
    Optional<SesionCaja> buscarSesionActivaPorTerminal(Long terminalId);
    
    // Consultas
    Optional<SesionCaja> buscarPorId(Long id);
    List<SesionCaja> listarPorFecha(LocalDate fecha);
    List<SesionCaja> listarPorTerminalYFecha(Long terminalId, LocalDate fecha);
    List<SesionCaja> listarPorUsuarioYFecha(Long usuarioId, LocalDateTime fechaInicio, LocalDateTime fechaFin);
    
    // Actualización de totales
    void actualizarTotalVentas(Long sesionId, BigDecimal monto);
    void actualizarTotalDevoluciones(Long sesionId, BigDecimal monto);
    void incrementarContadorDocumento(Long sesionId, String tipoDocumento);
    
    // Validaciones
    boolean usuarioTieneSesionAbierta(Long usuarioId);
    boolean terminalTieneSesionAbierta(Long terminalId);
}