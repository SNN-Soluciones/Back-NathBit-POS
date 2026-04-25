// src/main/java/com/snnsoluciones/backnathbitpos/service/V2SesionCajaService.java

package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.v2sesion.*;
import com.snnsoluciones.backnathbitpos.entity.V2SesionCaja;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface V2SesionCajaService {

    /**
     * Abre sesión de caja para un kiosko — sin usuario, de forma autónoma.
     * Si ya hay sesión abierta para la terminal, la retorna.
     */
    V2AbrirSesionResponse abrirSesionKiosko(Long terminalId);

    // ── Sesión ────────────────────────────────────────────────
    V2AbrirSesionResponse  abrirSesion(V2AbrirSesionRequest request, Long usuarioId);
    V2EstadoSesionResponse obtenerEstado(Long sesionId, Long usuarioId);

    // ── Turnos ────────────────────────────────────────────────
    V2TurnoResponse unirseATurno(Long sesionId, Long usuarioId);
    V2CerrarTurnoResponse cerrarTurno(Long turnoId, V2CerrarTurnoRequest request, Long usuarioId);
    V2TurnoResponse obtenerMiTurnoActivo(Long usuarioId);

    // ── Movimientos ───────────────────────────────────────────
    V2MovimientoResponse registrarMovimiento(Long turnoId, V2MovimientoRequest request, Long usuarioId);

    // ── Cálculo de fondo ──────────────────────────────────────
    BigDecimal calcularFondoActualGaveta(Long sesionId);

    V2EstadoSesionResponse obtenerEstadoPorTerminal(Long terminalId, Long usuarioId);
    Map<String, Object> obtenerUltimoFondoTerminal(Long terminalId);

    Page<V2BitacoraCajaSesionDTO> listarBitacora(V2BitacoraCajaFilterRequest filtros);

}