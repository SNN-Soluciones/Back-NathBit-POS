package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.SesionCaja;
import com.snnsoluciones.backnathbitpos.entity.Terminal;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.enums.EstadoSesion;
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
    
    @Override
    public SesionCaja abrirSesion(Long usuarioId, Long terminalId, BigDecimal montoInicial) {
        log.info("Abriendo sesión de caja para usuario: {} en terminal: {}", usuarioId, terminalId);
        
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
        log.info("Cerrando sesión de caja ID: {}", sesionId);
        
        SesionCaja sesion = sesionCajaRepository.findById(sesionId)
            .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));
        
        if (!sesion.puedeCerrarse()) {
            throw new RuntimeException("La sesión no puede cerrarse en su estado actual");
        }
        
        // Calcular diferencia
        BigDecimal montoEsperado = sesion.calcularMontoEsperado();
        BigDecimal diferencia = montoCierre.subtract(montoEsperado);
        
        // Actualizar datos de cierre
        sesion.setFechaHoraCierre(LocalDateTime.now());
        sesion.setMontoCierre(montoCierre);
        sesion.setDiferenciaCierre(diferencia);
        sesion.setObservacionesCierre(observaciones);
        sesion.setEstado(EstadoSesion.CERRADA);
        
        sesion = sesionCajaRepository.save(sesion);
        log.info("Sesión cerrada. Diferencia: {}", diferencia);
        
        return sesion;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<SesionCaja> buscarSesionActiva(Long usuarioId) {
        return sesionCajaRepository.findSesionAbiertaByUsuarioId(usuarioId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<SesionCaja> buscarSesionActivaPorTerminal(Long terminalId) {
        return sesionCajaRepository.findSesionAbiertaByTerminalId(terminalId);
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
}