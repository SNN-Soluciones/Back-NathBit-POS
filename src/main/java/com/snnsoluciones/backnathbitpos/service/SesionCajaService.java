package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.sesiones.ResumenCajaDetalladoDTO;
import com.snnsoluciones.backnathbitpos.entity.SesionCaja;

import com.snnsoluciones.backnathbitpos.enums.EstadoSesion;
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

    ResumenCajaDetalladoDTO obtenerResumenDetallado(Long sesionId);
    BigDecimal calcularMontoEsperado(SesionCaja sesion);

    // Consultas
    Optional<SesionCaja> buscarPorId(Long id);
    List<SesionCaja> listarPorFecha(LocalDate fecha);
    List<SesionCaja> listarPorTerminalYFecha(Long terminalId, LocalDate fecha);

    // Actualización de totales

    // Validaciones
    boolean usuarioTieneSesionAbierta(Long usuarioId);
    boolean terminalTieneSesionAbierta(Long terminalId);

    List<SesionCaja> buscarTodas();
    List<SesionCaja> buscarPorEstado(EstadoSesion estado);
    SesionCaja cerrarSesionAdmin(Long sesionId, BigDecimal montoCierre, String observaciones);
}