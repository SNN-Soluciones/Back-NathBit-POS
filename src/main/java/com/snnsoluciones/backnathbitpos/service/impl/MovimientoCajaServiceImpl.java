package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.movimiento.HistorialMovimientosResponse;
import com.snnsoluciones.backnathbitpos.dto.movimiento.MovimientoCajaDTO;
import com.snnsoluciones.backnathbitpos.dto.movimiento.RegistrarEntradaRequest;
import com.snnsoluciones.backnathbitpos.dto.movimiento.RegistrarSalidaRequest;
import com.snnsoluciones.backnathbitpos.entity.MovimientoCaja;
import com.snnsoluciones.backnathbitpos.entity.SesionCaja;
import com.snnsoluciones.backnathbitpos.enums.EstadoSesion;
import com.snnsoluciones.backnathbitpos.enums.TipoMovimientoCaja;
import com.snnsoluciones.backnathbitpos.repository.MovimientoCajaRepository;
import com.snnsoluciones.backnathbitpos.repository.SesionCajaRepository;
import com.snnsoluciones.backnathbitpos.repository.SesionCajaUsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.MovimientoCajaService;
import java.util.stream.Collectors;
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
    private final SesionCajaUsuarioRepository sesionCajaUsuarioRepository;

    @Override
    public MovimientoCaja registrarVale(Long sesionId, BigDecimal monto, String concepto) {
        log.info("Registrando vale para sesión {} por monto {}", sesionId, monto);

        // Solo JEFE_CAJAS o superior puede autorizar vales
        if (!securityContext.hasAnyRole("CAJERO", "JEFE_CAJAS", "ADMIN", "SUPER_ADMIN", "ROOT", "SOPORTE")) {
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

        // Linkear al turno activo del usuario si existe
        sesionCajaUsuarioRepository
            .findTurnoActivoUsuarioEnSesion(
                securityContext.getCurrentUserId(),
                sesionId)
            .ifPresent(movimiento::setSesionCajaUsuario);

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

    @Override
    public MovimientoCajaDTO registrarSalida(Long sesionId, RegistrarSalidaRequest request) {
        log.info("Registrando salida tipo {} para sesión {}", request.getTipoSalida(), sesionId);

        // Validar permisos
        validarPermisos();

        // Validar que el tipo sea una salida
        if (!request.getTipoSalida().esSalida()) {
            throw new IllegalArgumentException("El tipo debe ser una salida de efectivo");
        }

        // Validar sesión abierta
        SesionCaja sesion = validarSesionAbierta(sesionId);

        // Preparar el concepto según el tipo
        String concepto = prepararConcepto(request);

        // Crear movimiento
        MovimientoCaja movimiento = new MovimientoCaja();
        movimiento.setSesionCaja(sesion);
        movimiento.setTipoMovimiento(request.getTipoSalida());
        movimiento.setMonto(request.getMonto());
        movimiento.setConcepto(concepto);
        movimiento.setAutorizadoPorId(securityContext.getCurrentUserId());
        movimiento.setFechaHora(LocalDateTime.now());
        movimiento.setObservaciones(request.getObservaciones());

        // Linkear al turno activo del usuario si existe
        sesionCajaUsuarioRepository
            .findTurnoActivoUsuarioEnSesion(
                securityContext.getCurrentUserId(),
                sesionId)
            .ifPresent(movimiento::setSesionCajaUsuario);

        MovimientoCaja saved = movimientoCajaRepository.save(movimiento);

        log.info("Salida registrada exitosamente: ID {}", saved.getId());

        return mapearADTO(saved, request);
    }

    @Override
    public MovimientoCajaDTO registrarEntrada(Long sesionId, RegistrarEntradaRequest request) {
        log.info("Registrando entrada de efectivo para sesión {}", sesionId);

        // Validar permisos
        validarPermisos();

        // Validar sesión abierta
        SesionCaja sesion = validarSesionAbierta(sesionId);

        // Crear movimiento
        MovimientoCaja movimiento = new MovimientoCaja();
        movimiento.setSesionCaja(sesion);
        movimiento.setTipoMovimiento(TipoMovimientoCaja.ENTRADA_EFECTIVO);
        movimiento.setMonto(request.getMonto());
        movimiento.setConcepto(request.getConcepto());
        movimiento.setAutorizadoPorId(securityContext.getCurrentUserId());
        movimiento.setFechaHora(LocalDateTime.now());
        movimiento.setObservaciones(request.getObservaciones());

        sesionCajaUsuarioRepository
            .findTurnoActivoUsuarioEnSesion(
                securityContext.getCurrentUserId(),
                sesionId)
            .ifPresent(movimiento::setSesionCajaUsuario);

        MovimientoCaja saved = movimientoCajaRepository.save(movimiento);

        log.info("Entrada registrada exitosamente: ID {}", saved.getId());

        return mapearADTO(saved, null);
    }

    @Override
    public HistorialMovimientosResponse obtenerHistorialCompleto(Long sesionId) {
        log.info("Obteniendo historial completo de sesión {}", sesionId);

        List<MovimientoCaja> movimientos = obtenerMovimientosPorSesion(sesionId);

        // Calcular totales
        BigDecimal totalEntradas = calcularTotalPorTipo(movimientos, true, false);
        BigDecimal totalSalidas = calcularTotalPorTipo(movimientos, false, true);
        BigDecimal totalVales = calcularTotalPorTipoEspecifico(movimientos, TipoMovimientoCaja.SALIDA_VALE);
        BigDecimal totalArqueos = calcularTotalPorTipoEspecifico(movimientos, TipoMovimientoCaja.SALIDA_ARQUEO);
        BigDecimal totalPagosProveedores = calcularTotalPorTipoEspecifico(movimientos, TipoMovimientoCaja.SALIDA_PAGO_PROVEEDOR);
        BigDecimal totalOtros = calcularTotalPorTipoEspecifico(movimientos, TipoMovimientoCaja.SALIDA_OTROS);

        List<MovimientoCajaDTO> movimientosDTO = movimientos.stream()
            .map(m -> mapearADTO(m, null))
            .collect(Collectors.toList());

        return HistorialMovimientosResponse.builder()
            .sesionCajaId(sesionId)
            .movimientos(movimientosDTO)
            .totalEntradas(totalEntradas)
            .totalSalidas(totalSalidas)
            .totalVales(totalVales)
            .totalArqueos(totalArqueos)
            .totalPagosProveedores(totalPagosProveedores)
            .totalOtros(totalOtros)
            .cantidadMovimientos(movimientos.size())
            .build();
    }


    @Override
    public BigDecimal obtenerTotalSalidasPorTipo(Long sesionId, String tipoMovimiento) {
        try {
            TipoMovimientoCaja tipo = TipoMovimientoCaja.valueOf(tipoMovimiento);
            return movimientoCajaRepository.sumBySesionIdAndTipo(sesionId, tipo);
        } catch (IllegalArgumentException e) {
            log.error("Tipo de movimiento inválido: {}", tipoMovimiento);
            return BigDecimal.ZERO;
        }
    }

    // ===== MÉTODOS PRIVADOS HELPER =====

    private void validarPermisos() {
        if (!securityContext.hasAnyRole("CAJERO", "JEFE_CAJAS", "ADMIN", "SUPER_ADMIN", "ROOT", "SOPORTE")) {
            throw new RuntimeException("No tiene permisos para registrar movimientos de caja");
        }
    }

    private String prepararConcepto(RegistrarSalidaRequest request) {
      return switch (request.getTipoSalida()) {
        case SALIDA_ARQUEO -> "Arqueo de caja";
        case SALIDA_PAGO_PROVEEDOR -> "Pago a proveedor: " + request.getNombreProveedor();
        case SALIDA_OTROS -> request.getMotivo();
        default -> request.getTipoSalida().getDescripcion();
      };
    }

    private MovimientoCajaDTO mapearADTO(MovimientoCaja movimiento, RegistrarSalidaRequest request) {
        MovimientoCajaDTO dto = MovimientoCajaDTO.builder()
            .id(movimiento.getId())
            .tipoMovimiento(movimiento.getTipoMovimiento().name())
            .descripcionTipo(movimiento.getTipoMovimiento().getDescripcion())
            .monto(movimiento.getMonto())
            .concepto(movimiento.getConcepto())
            .autorizadoPorId(movimiento.getAutorizadoPorId())
            .fechaHora(movimiento.getFechaHora())
            .observaciones(movimiento.getObservaciones())
            .esEntrada(movimiento.getTipoMovimiento().esEntrada())
            .esSalida(movimiento.getTipoMovimiento().esSalida())
            .build();

        // Agregar campos específicos si hay request
        if (request != null) {
            dto.setNombreProveedor(request.getNombreProveedor());
            dto.setMotivo(request.getMotivo());
        }

        return dto;
    }

    private BigDecimal calcularTotalPorTipo(List<MovimientoCaja> movimientos, boolean entradas, boolean salidas) {
        return movimientos.stream()
            .filter(m -> (entradas && m.getTipoMovimiento().esEntrada()) ||
                (salidas && m.getTipoMovimiento().esSalida()))
            .map(MovimientoCaja::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularTotalPorTipoEspecifico(List<MovimientoCaja> movimientos, TipoMovimientoCaja tipo) {
        return movimientos.stream()
            .filter(m -> m.getTipoMovimiento() == tipo)
            .map(MovimientoCaja::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}