package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.MovimientoCaja;
import com.snnsoluciones.backnathbitpos.entity.SesionCaja;
import com.snnsoluciones.backnathbitpos.enums.EstadoSesion;
import com.snnsoluciones.backnathbitpos.enums.TipoMovimientoCaja;
import com.snnsoluciones.backnathbitpos.repository.MovimientoCajaRepository;
import com.snnsoluciones.backnathbitpos.repository.SesionCajaRepository;
import com.snnsoluciones.backnathbitpos.service.MovimientoCajaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MovimientoCajaServiceImpl implements MovimientoCajaService {

    private final MovimientoCajaRepository movimientoCajaRepository;
    private final SesionCajaRepository sesionCajaRepository;
    private final SecurityContextService securityContext;

    @Override
    public MovimientoCaja registrarVale(Long sesionId, BigDecimal monto, String concepto) {
        log.info("Registrando vale para sesión {} por monto {}", sesionId, monto);

        // Solo JEFE_CAJAS o superior puede autorizar vales
        if (!securityContext.hasAnyRole("JEFE_CAJAS", "ADMIN", "SUPER_ADMIN", "ROOT", "SOPORTE")) {
            throw new RuntimeException("No tiene permisos para autorizar vales");
        }

        // Validar que la sesión esté abierta
        SesionCaja sesion = validarSesionAbierta(sesionId);

        // Crear el movimiento
        MovimientoCaja movimiento = new MovimientoCaja();
        movimiento.setSesionCaja(sesion);
        movimiento.setTipoMovimiento(TipoMovimientoCaja.SALIDA_VALE);
        movimiento.setMonto(monto);
        movimiento.setConcepto(concepto);
        movimiento.setAutorizadoPorId(securityContext.getCurrentUserId());
        movimiento.setFechaHora(LocalDateTime.now());

        return movimientoCajaRepository.save(movimiento);
    }

    @Override
    public List<MovimientoCaja> obtenerMovimientosPorSesion(Long sesionId) {
        log.debug("Obteniendo movimientos de sesión {}", sesionId);

        // Validar que el usuario tenga acceso a ver esta sesión
        SesionCaja sesion = sesionCajaRepository.findById(sesionId)
            .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        // Si es supervisor puede ver todo
        if (!securityContext.isSupervisor()) {
            // Si es cajero, solo puede ver su propia sesión
            if (!sesion.getUsuario().getId().equals(securityContext.getCurrentUserId())) {
                throw new RuntimeException("No tiene permisos para ver movimientos de esta sesión");
            }
        }

        return movimientoCajaRepository.findBySesionCajaIdOrderByFechaHoraDesc(sesionId);
    }

    @Override
    public BigDecimal obtenerTotalVales(Long sesionId) {
        log.debug("Calculando total de vales para sesión {}", sesionId);
        return movimientoCajaRepository.sumBySesionIdAndTipo(sesionId, TipoMovimientoCaja.SALIDA_VALE);
    }

    /**
     * Valida que la sesión exista y esté abierta
     */
    private SesionCaja validarSesionAbierta(Long sesionId) {
        SesionCaja sesion = sesionCajaRepository.findById(sesionId)
            .orElseThrow(() -> new RuntimeException("Sesión de caja no encontrada"));

        if (sesion.getEstado() != EstadoSesion.ABIERTA) {
            throw new RuntimeException("La sesión de caja no está abierta");
        }

        return sesion;
    }
}