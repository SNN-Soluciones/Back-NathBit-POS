package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.sesiones.ResumenCajaDetalladoDTO;
import com.snnsoluciones.backnathbitpos.entity.SesionCaja;
import com.snnsoluciones.backnathbitpos.entity.Terminal;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.enums.EstadoSesion;
import com.snnsoluciones.backnathbitpos.enums.TipoMovimientoCaja;
import com.snnsoluciones.backnathbitpos.repository.MovimientoCajaRepository;
import com.snnsoluciones.backnathbitpos.repository.SesionCajaRepository;
import com.snnsoluciones.backnathbitpos.repository.TerminalRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.SesionCajaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SesionCajaServiceImpl implements SesionCajaService {
    
    private final SesionCajaRepository sesionCajaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TerminalRepository terminalRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;
    private final SecurityContextService securityContext;

    @Override
    public SesionCaja abrirSesion(Long usuarioId, Long terminalId, BigDecimal montoInicial) {
        log.info("Abriendo sesión de caja para usuario: {} en terminal: {}", usuarioId, terminalId);

        if (!puedeAbrirCaja()) {
            throw new RuntimeException("No tiene permisos para abrir caja");
        }
        
        // Validar que no tenga sesión abierta
        if (usuarioTieneSesionAbierta(usuarioId)) {
            throw new RuntimeException("El usuario ya tiene una sesión de caja abierta");
        }
        
        // Validar que la terminal no esté ocupada
        if (terminalTieneSesionAbierta(terminalId)) {
            throw new RuntimeException("La terminal ya tiene una sesión abierta");
        }
        
        // Obtener entidades
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
        Terminal terminal = terminalRepository.findById(terminalId)
            .orElseThrow(() -> new RuntimeException("Terminal no encontrada"));
        
        if (!terminal.getActiva()) {
            throw new RuntimeException("La terminal no está activa");
        }
        
        // Crear nueva sesión
        SesionCaja sesion = new SesionCaja();
        sesion.setUsuario(usuario);
        sesion.setTerminal(terminal);
        sesion.setFechaHoraApertura(LocalDateTime.now());
        sesion.setMontoInicial(montoInicial != null ? montoInicial : BigDecimal.ZERO);
        sesion.setEstado(EstadoSesion.ABIERTA);
        sesion.setObservacionesApertura("");
        
        // Inicializar contadores
        sesion.setTotalVentas(BigDecimal.ZERO);
        sesion.setTotalDevoluciones(BigDecimal.ZERO);
        sesion.setTotalEfectivo(BigDecimal.ZERO);
        sesion.setTotalTarjeta(BigDecimal.ZERO);
        sesion.setTotalTransferencia(BigDecimal.ZERO);
        sesion.setTotalOtros(BigDecimal.ZERO);
        sesion.setCantidadFacturas(0);
        sesion.setCantidadTiquetes(0);
        sesion.setCantidadNotasCredito(0);
        
        sesion = sesionCajaRepository.save(sesion);
        log.info("Sesión de caja abierta con ID: {}", sesion.getId());
        
        return sesion;
    }

    @Override
    public SesionCaja cerrarSesion(Long sesionId, BigDecimal montoCierre, String observaciones) {
        // Validar permisos
        if (!puedeCerrarCaja(sesionId)) {
            throw new RuntimeException("No tiene permisos para cerrar esta caja");
        }

        SesionCaja sesion = sesionCajaRepository.findById(sesionId)
            .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        // 🔥 AQUÍ USAS calcularMontoEsperado
        BigDecimal montoEsperado = calcularMontoEsperado(sesion);

        // Validar diferencia
        BigDecimal diferencia = montoCierre.subtract(montoEsperado);

        // Si hay diferencia significativa y no es supervisor
        if (diferencia.abs().compareTo(new BigDecimal("10000")) > 0 && !securityContext.isSupervisor()) {
            throw new RuntimeException(String.format(
                "Diferencia de ₡%.2f requiere autorización de supervisor. Esperado: ₡%.2f, Cierre: ₡%.2f",
                diferencia, montoEsperado, montoCierre
            ));
        }

        // Actualizar sesión
        sesion.setFechaHoraCierre(LocalDateTime.now());
        sesion.setMontoCierre(montoCierre);
        sesion.setDiferenciaCierre(diferencia);
        sesion.setObservacionesCierre(observaciones);
        sesion.setEstado(EstadoSesion.CERRADA);

        return sesionCajaRepository.save(sesion);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<SesionCaja> buscarSesionActiva(Long usuarioId) {
        return sesionCajaRepository.findSesionAbiertaByUsuarioId(usuarioId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<SesionCaja> buscarPorId(Long id) {
        return sesionCajaRepository.findById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SesionCaja> listarPorFecha(LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);
        return sesionCajaRepository.findByFechaRango(inicio, fin);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SesionCaja> listarPorTerminalYFecha(Long terminalId, LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);
        return sesionCajaRepository.findByTerminalIdAndFechaRango(terminalId, inicio, fin);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SesionCaja> listarPorUsuarioYFecha(Long usuarioId, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        // TODO: Implementar query en repository si es necesario
        return List.of();
    }
    
    @Override
    public void actualizarTotalVentas(Long sesionId, BigDecimal monto) {
        SesionCaja sesion = buscarPorId(sesionId)
            .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));
        
        sesion.setTotalVentas(sesion.getTotalVentas().add(monto));
        
        // Actualizar total por medio de pago (asumiendo efectivo por defecto)
        // TODO: Recibir el medio de pago como parámetro
        sesion.setTotalEfectivo(sesion.getTotalEfectivo().add(monto));
        
        sesionCajaRepository.save(sesion);
    }
    
    @Override
    public void actualizarTotalDevoluciones(Long sesionId, BigDecimal monto) {
        SesionCaja sesion = buscarPorId(sesionId)
            .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));
        
        sesion.setTotalDevoluciones(sesion.getTotalDevoluciones().add(monto));
        sesionCajaRepository.save(sesion);
    }
    
    @Override
    public void incrementarContadorDocumento(Long sesionId, String tipoDocumento) {
        SesionCaja sesion = buscarPorId(sesionId)
            .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));
        
        switch (tipoDocumento) {
            case "FE":
            case "FEE":
                sesion.setCantidadFacturas(sesion.getCantidadFacturas() + 1);
                break;
            case "TE":
                sesion.setCantidadTiquetes(sesion.getCantidadTiquetes() + 1);
                break;
            case "NC":
                sesion.setCantidadNotasCredito(sesion.getCantidadNotasCredito() + 1);
                break;
        }
        
        sesionCajaRepository.save(sesion);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean usuarioTieneSesionAbierta(Long usuarioId) {
        return sesionCajaRepository.existsSesionAbiertaByUsuarioId(usuarioId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean terminalTieneSesionAbierta(Long terminalId) {
        return sesionCajaRepository.findSesionAbiertaByTerminalId(terminalId).isPresent();
    }

    @Override
    public Optional<SesionCaja> buscarSesionActivaPorTerminal(Long terminalId) {
        return sesionCajaRepository.findByTerminalIdAndEstado(terminalId, EstadoSesion.ABIERTA);
    }

    @Override
    public List<SesionCaja> listarSesionesDia(Long sucursalId, LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);

        return sesionCajaRepository.findBySucursalIdAndFechaBetween(
            sucursalId, inicio, fin
        );
    }

    @Override
    public boolean validarCierreDia(Long terminalId) {
        // Verificar si hay sesiones del día anterior sin cerrar
        LocalDate ayer = LocalDate.now().minusDays(1);
        LocalDateTime inicioAyer = ayer.atStartOfDay();
        LocalDateTime finAyer = ayer.atTime(LocalTime.MAX);

        return !sesionCajaRepository.existsByTerminalIdAndEstadoAndFechaBetween(
            terminalId, EstadoSesion.ABIERTA, inicioAyer, finAyer
        );
    }

    @Override
    public ResumenCajaDetalladoDTO obtenerResumenDetallado(Long sesionId) {
        SesionCaja sesion = sesionCajaRepository.findById(sesionId)
            .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        // Validar acceso
        if (!puedeVerResumen(sesion)) {
            throw new RuntimeException("No tiene permisos para ver este resumen");
        }

        ResumenCajaDetalladoDTO resumen = new ResumenCajaDetalladoDTO();
        resumen.setSesionId(sesionId);
        resumen.setTerminal(sesion.getTerminal().getNombre());
        resumen.setCajero(sesion.getUsuario().getNombre().concat(" ").concat(sesion.getUsuario().getApellidos()));
        resumen.setFechaApertura(sesion.getFechaHoraApertura());
        resumen.setFechaCierre(sesion.getFechaHoraCierre());

        // Montos básicos
        resumen.setMontoInicial(sesion.getMontoInicial());
        resumen.setVentasEfectivo(sesion.getTotalEfectivo());
        resumen.setVentasTarjeta(sesion.getTotalTarjeta());
        resumen.setVentasTransferencia(sesion.getTotalTransferencia());
        resumen.setVentasOtros(sesion.getTotalOtros());

        // Movimientos
        resumen.setEntradasAdicionales(
            movimientoCajaRepository.sumBySesionIdAndTipo(sesionId, TipoMovimientoCaja.ENTRADA_ADICIONAL)
        );
        resumen.setVales(
            movimientoCajaRepository.sumBySesionIdAndTipo(sesionId, TipoMovimientoCaja.SALIDA_VALE)
        );
        resumen.setDepositos(
            movimientoCajaRepository.sumBySesionIdAndTipo(sesionId, TipoMovimientoCaja.SALIDA_DEPOSITO)
        );

        // 🔥 AQUÍ TAMBIÉN USAS calcularMontoEsperado
        resumen.setMontoEsperado(calcularMontoEsperado(sesion));
        resumen.setMontoCierre(sesion.getMontoCierre());

        // Contadores
        resumen.setCantidadFacturas(sesion.getCantidadFacturas());
        resumen.setCantidadTiquetes(sesion.getCantidadTiquetes());
        resumen.setCantidadNotasCredito(sesion.getCantidadNotasCredito());

        // Lista de movimientos
        resumen.setMovimientos(
            movimientoCajaRepository.findBySesionCajaIdOrderByFechaHoraDesc(sesionId)
        );

        return resumen;
    }

    private boolean puedeVerResumen(SesionCaja sesion) {
        // Supervisores pueden ver todo
        if (securityContext.isSupervisor()) {
            return true;
        }
        // Cajero solo puede ver su propia sesión
        return sesion.getUsuario().getId().equals(securityContext.getCurrentUserId());
    }

    private boolean puedeAbrirCaja() {
        return securityContext.hasAnyRole("CAJERO", "JEFE_CAJAS", "ADMIN", "SUPER_ADMIN", "ROOT");
    }

    private boolean puedeCerrarCaja(Long sesionId) {
        // JEFE_CAJAS y superiores pueden cerrar cualquier caja
        if (securityContext.isSupervisor()) {
            return true;
        }

        // CAJERO solo puede cerrar su propia caja
        SesionCaja sesion = sesionCajaRepository.findById(sesionId).orElse(null);
        return sesion != null &&
            sesion.getUsuario().getId().equals(securityContext.getCurrentUserId());
    }

    @Override
    public BigDecimal obtenerTotalVales(Long sesionId) {
        // Implementar query para sumar vales/salidas
        return movimientoCajaRepository
            .sumBySesionIdAndTipo(sesionId, TipoMovimientoCaja.SALIDA_VALE);
    }

    @Override
    public BigDecimal calcularMontoEsperado(SesionCaja sesion) {
        // Monto inicial
        BigDecimal montoEsperado = sesion.getMontoInicial();

        // + Ventas en efectivo
        montoEsperado = montoEsperado.add(sesion.getTotalEfectivo());

        // + Entradas adicionales (si agregaron más efectivo durante el día)
        BigDecimal entradas = movimientoCajaRepository
            .sumBySesionIdAndTipo(sesion.getId(), TipoMovimientoCaja.ENTRADA_ADICIONAL);
        montoEsperado = montoEsperado.add(entradas);

        // - Salidas (vales y depósitos)
        BigDecimal salidas = movimientoCajaRepository.sumSalidasBySesionId(sesion.getId());
        montoEsperado = montoEsperado.subtract(salidas);

        return montoEsperado;
    }
}