package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.sesion.CerrarSesionRequest;
import com.snnsoluciones.backnathbitpos.dto.sesion.OpcionesImpresionCierreDTO;
import com.snnsoluciones.backnathbitpos.dto.sesiones.ResumenCajaDetalladoDTO;
import com.snnsoluciones.backnathbitpos.dto.sesiones.SesionCajaDTO;
import com.snnsoluciones.backnathbitpos.entity.SesionCaja;

import com.snnsoluciones.backnathbitpos.enums.EstadoSesion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SesionCajaService {
    
    // Gestión de sesiones
    SesionCaja abrirSesion(Long usuarioId, Long terminalId, BigDecimal montoInicial);
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

    SesionCaja cerrarSesion(Long id, BigDecimal montoCierre, CerrarSesionRequest request, String observaciones,
        List<CerrarSesionRequest.DenominacionDTO> denominaciones);

    Page<SesionCajaDTO> listarPorSucursal(Long sucursalId, Pageable pageable);

    /**
     * 🆕 Busca el fondo de caja de la última sesión cerrada de una terminal
     * Útil para sugerir el montoInicial al abrir nueva sesión
     *
     * @param terminalId ID de la terminal
     * @return Optional con la última sesión cerrada
     */
    Optional<SesionCaja> buscarUltimaSesionCerrada(Long terminalId);
    byte[] generarPdfCierre(Long sesionId, OpcionesImpresionCierreDTO opciones);
    void enviarCierrePorEmail(Long sesionId, OpcionesImpresionCierreDTO opciones);
}