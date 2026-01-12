package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.asistencia.AsistenciaDTO;
import com.snnsoluciones.backnathbitpos.dto.asistencia.MarcarAsistenciaRequest;
import com.snnsoluciones.backnathbitpos.dto.asistencia.MarcarAsistenciaResponse;
import com.snnsoluciones.backnathbitpos.entity.Asistencia;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.AsistenciaRepository;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.AsistenciaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de asistencias
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsistenciaServiceImpl implements AsistenciaService {
    
    private final AsistenciaRepository asistenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    
    @Override
    @Transactional
    public MarcarAsistenciaResponse marcarAsistencia(
            Long usuarioId,
            Long empresaId,
            Long sucursalId,
            MarcarAsistenciaRequest request) {
        
        log.info("Marcando {} - Usuario: {}, Empresa: {}, Sucursal: {}", 
            request.getTipo(), usuarioId, empresaId, sucursalId);
        
        // 1. Validar usuario
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        // 2. Validar empresa
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        
        // 3. Validar sucursal
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
        
        LocalDate hoy = LocalDate.now();
        
        // 4. Buscar asistencia existente HOY
        Optional<Asistencia> asistenciaExistente = asistenciaRepository
            .findByUsuarioIdAndFecha(usuarioId, hoy);
        
        if ("ENTRADA".equals(request.getTipo())) {
            return marcarEntrada(usuario, empresa, sucursal, asistenciaExistente, request);
        } else if ("SALIDA".equals(request.getTipo())) {
            return marcarSalida(asistenciaExistente, request);
        } else {
            throw new BadRequestException("Tipo inválido. Debe ser ENTRADA o SALIDA");
        }
    }
    
    /**
     * Marca la entrada de un usuario
     */
    private MarcarAsistenciaResponse marcarEntrada(
            Usuario usuario,
            Empresa empresa,
            Sucursal sucursal,
            Optional<Asistencia> asistenciaExistente,
            MarcarAsistenciaRequest request) {
        
        // Validar que no tenga entrada activa
        if (asistenciaExistente.isPresent() && asistenciaExistente.get().tieneEntradaActiva()) {
            throw new BadRequestException("Ya existe una entrada registrada hoy");
        }
        
        Asistencia asistencia;
        LocalDateTime ahora = LocalDateTime.now();
        
        if (asistenciaExistente.isPresent()) {
            // Ya había marcado entrada y salida hoy, crear nueva entrada
            asistencia = Asistencia.builder()
                .usuario(usuario)
                .empresa(empresa)
                .sucursal(sucursal)
                .fecha(LocalDate.now())
                .horaEntrada(ahora)
                .observaciones(request.getObservaciones())
                .build();
        } else {
            // Primera entrada del día
            asistencia = Asistencia.builder()
                .usuario(usuario)
                .empresa(empresa)
                .sucursal(sucursal)
                .fecha(LocalDate.now())
                .horaEntrada(ahora)
                .observaciones(request.getObservaciones())
                .build();
        }
        
        asistencia = asistenciaRepository.save(asistencia);
        
        log.info("Entrada marcada - Usuario: {}, Hora: {}", usuario.getId(), ahora);
        
        return MarcarAsistenciaResponse.builder()
            .id(asistencia.getId())
            .tipo("ENTRADA")
            .hora(ahora)
            .fecha(asistencia.getFecha())
            .horaEntrada(asistencia.getHoraEntrada())
            .horaSalida(null)
            .build();
    }
    
    /**
     * Marca la salida de un usuario
     */
    private MarcarAsistenciaResponse marcarSalida(
            Optional<Asistencia> asistenciaExistente,
            MarcarAsistenciaRequest request) {
        
        // Validar que exista entrada hoy
        if (asistenciaExistente.isEmpty()) {
            throw new BadRequestException("No hay entrada registrada para marcar salida");
        }
        
        Asistencia asistencia = asistenciaExistente.get();
        
        // Validar que no haya salido ya
        if (!asistencia.tieneEntradaActiva()) {
            throw new BadRequestException("Ya se marcó salida anteriormente");
        }
        
        LocalDateTime ahora = LocalDateTime.now();
        asistencia.marcarSalida();
        
        if (request.getObservaciones() != null) {
            asistencia.setObservaciones(request.getObservaciones());
        }
        
        asistencia = asistenciaRepository.save(asistencia);
        
        log.info("Salida marcada - Usuario: {}, Hora: {}", 
            asistencia.getUsuario().getId(), ahora);
        
        return MarcarAsistenciaResponse.builder()
            .id(asistencia.getId())
            .tipo("SALIDA")
            .hora(ahora)
            .fecha(asistencia.getFecha())
            .horaEntrada(asistencia.getHoraEntrada())
            .horaSalida(asistencia.getHoraSalida())
            .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean tieneEntradaActiva(Long usuarioId) {
        return tieneEntradaActivaEnFecha(usuarioId, LocalDate.now());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean tieneEntradaActivaEnFecha(Long usuarioId, LocalDate fecha) {
        return asistenciaRepository.tieneEntradaActiva(usuarioId, fecha);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AsistenciaDTO> listarAsistenciasPorEmpresaYFecha(Long empresaId, LocalDate fecha) {
        List<Asistencia> asistencias = asistenciaRepository.findByEmpresaIdAndFecha(empresaId, fecha);
        
        return asistencias.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AsistenciaDTO> listarUsuariosPresentes(Long empresaId, LocalDate fecha) {
        List<Asistencia> asistencias = asistenciaRepository
            .findEntradasActivasByEmpresa(empresaId, fecha);
        
        return asistencias.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AsistenciaDTO> obtenerHistorial(
            Long usuarioId, 
            LocalDate fechaInicio, 
            LocalDate fechaFin) {
        
        List<Asistencia> asistencias = asistenciaRepository
            .findByUsuarioIdAndFechaBetween(usuarioId, fechaInicio, fechaFin);
        
        return asistencias.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    // ==================== MÉTODOS PRIVADOS ====================
    
    /**
     * Mapea Asistencia a DTO
     */
    private AsistenciaDTO mapToDTO(Asistencia asistencia) {
        return AsistenciaDTO.builder()
            .id(asistencia.getId())
            .usuarioId(asistencia.getUsuario().getId())
            .usuarioNombre(asistencia.getUsuario().getNombre())
            .usuarioNombreCompleto(asistencia.getUsuario().getNombre() + " " + 
                (asistencia.getUsuario().getApellidos() != null ? asistencia.getUsuario().getApellidos() : ""))
            .fecha(asistencia.getFecha())
            .horaEntrada(asistencia.getHoraEntrada())
            .horaSalida(asistencia.getHoraSalida())
            .tieneEntradaActiva(asistencia.tieneEntradaActiva())
            .observaciones(asistencia.getObservaciones())
            .sucursalId(asistencia.getSucursal().getId())
            .sucursalNombre(asistencia.getSucursal().getNombre())
            .build();
    }
}