package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.impresion.ImpresoraAndroidDTO;
import com.snnsoluciones.backnathbitpos.entity.ImpresoraAndroid;
import com.snnsoluciones.backnathbitpos.entity.ImpresoraAndroid.TipoUsoImpresora;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.repository.ImpresoraAndroidRepository;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.ImpresoraAndroidService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ImpresoraAndroidServiceImpl implements ImpresoraAndroidService {

    private final ImpresoraAndroidRepository impresoraRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    public ImpresoraAndroidDTO crear(ImpresoraAndroidDTO dto, Long usuarioId) {
        // Validar sucursal
        Sucursal sucursal = sucursalRepository.findById(dto.getSucursalId())
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));

        // Validar nombre duplicado
        impresoraRepository.findBySucursalIdAndNombre(dto.getSucursalId(), dto.getNombre())
                .ifPresent(i -> {
                    throw new RuntimeException("Ya existe una impresora con ese nombre en la sucursal");
                });

        // Validar IP duplicada
        impresoraRepository.findBySucursalIdAndIp(dto.getSucursalId(), dto.getIp())
                .ifPresent(i -> {
                    throw new RuntimeException("Ya existe una impresora con esa IP en la sucursal");
                });

        // Validar ancho de papel
        if (dto.getAnchoPapel() != 58 && dto.getAnchoPapel() != 80) {
            throw new RuntimeException("El ancho de papel debe ser 58 o 80 mm");
        }

        // Si se marca como predeterminada, quitar flag de otras
        if (Boolean.TRUE.equals(dto.getPredeterminada())) {
            quitarPredeterminada(dto.getSucursalId(), dto.getTipoUso());
        }

        // Crear entidad
        ImpresoraAndroid impresora = new ImpresoraAndroid();
        impresora.setSucursal(sucursal);
        impresora.setNombre(dto.getNombre());
        impresora.setTipo(dto.getTipo());
        impresora.setIp(dto.getIp());
        impresora.setPuerto(dto.getPuerto() != null ? dto.getPuerto() : 9100);
        impresora.setAnchoPapel(dto.getAnchoPapel());
        impresora.setTipoUso(dto.getTipoUso());
        impresora.setPredeterminada(dto.getPredeterminada() != null ? dto.getPredeterminada() : false);
        impresora.setActiva(dto.getActiva() != null ? dto.getActiva() : true);

        // Auditoría
        if (usuarioId != null) {
            Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
            impresora.setUsuarioCreacion(usuario);
            impresora.setUsuarioActualizacion(usuario);
        }

        impresora = impresoraRepository.save(impresora);

        return toDTO(impresora);
    }

    @Override
    public ImpresoraAndroidDTO actualizar(Long id, ImpresoraAndroidDTO dto, Long usuarioId) {
        ImpresoraAndroid impresora = impresoraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Impresora no encontrada"));

        // Validar nombre duplicado (excepto la actual)
        if (impresoraRepository.existsByNombreExceptoId(
                impresora.getSucursal().getId(), 
                dto.getNombre(), 
                id)) {
            throw new RuntimeException("Ya existe una impresora con ese nombre en la sucursal");
        }

        // Validar IP duplicada (excepto la actual)
        if (impresoraRepository.existsByIpExceptoId(
                impresora.getSucursal().getId(), 
                dto.getIp(), 
                id)) {
            throw new RuntimeException("Ya existe una impresora con esa IP en la sucursal");
        }

        // Validar ancho de papel
        if (dto.getAnchoPapel() != 58 && dto.getAnchoPapel() != 80) {
            throw new RuntimeException("El ancho de papel debe ser 58 o 80 mm");
        }

        // Si se marca como predeterminada, quitar flag de otras
        if (Boolean.TRUE.equals(dto.getPredeterminada()) && 
            !Boolean.TRUE.equals(impresora.getPredeterminada())) {
            quitarPredeterminada(impresora.getSucursal().getId(), dto.getTipoUso());
        }

        // Actualizar campos
        impresora.setNombre(dto.getNombre());
        impresora.setTipo(dto.getTipo());
        impresora.setIp(dto.getIp());
        impresora.setPuerto(dto.getPuerto());
        impresora.setAnchoPapel(dto.getAnchoPapel());
        impresora.setTipoUso(dto.getTipoUso());
        impresora.setPredeterminada(dto.getPredeterminada());
        impresora.setActiva(dto.getActiva());

        // Auditoría
        if (usuarioId != null) {
            Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
            impresora.setUsuarioActualizacion(usuario);
        }

        impresora = impresoraRepository.save(impresora);

        return toDTO(impresora);
    }

    @Override
    public void eliminar(Long id) {
        ImpresoraAndroid impresora = impresoraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Impresora no encontrada"));
        
        impresoraRepository.delete(impresora);
    }

    @Override
    @Transactional(readOnly = true)
    public ImpresoraAndroidDTO obtenerPorId(Long id) {
        ImpresoraAndroid impresora = impresoraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Impresora no encontrada"));
        
        return toDTO(impresora);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImpresoraAndroidDTO> listarPorSucursal(Long sucursalId) {
        return impresoraRepository.findBySucursalId(sucursalId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImpresoraAndroidDTO> listarActivasPorSucursal(Long sucursalId) {
        return impresoraRepository.findBySucursalIdAndActivaTrue(sucursalId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImpresoraAndroidDTO> listarPorTipoUso(Long sucursalId, TipoUsoImpresora tipoUso) {
        return impresoraRepository.findBySucursalIdAndTipoUso(sucursalId, tipoUso)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ImpresoraAndroidDTO obtenerPredeterminada(Long sucursalId, TipoUsoImpresora tipoUso) {
        if (tipoUso == null) {
            return impresoraRepository.findPredeterminadaGeneral(sucursalId)
                    .map(this::toDTO)
                    .orElse(null);
        }
        
        return impresoraRepository.findBySucursalIdAndTipoUsoAndPredeterminadaTrue(sucursalId, tipoUso)
                .map(this::toDTO)
                .orElse(null);
    }

    @Override
    public ImpresoraAndroidDTO establecerComoPredeterminada(Long id, Long usuarioId) {
        ImpresoraAndroid impresora = impresoraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Impresora no encontrada"));

        // Quitar flag de otras impresoras del mismo tipo
        quitarPredeterminada(impresora.getSucursal().getId(), impresora.getTipoUso());

        // Establecer como predeterminada
        impresora.setPredeterminada(true);

        // Auditoría
        if (usuarioId != null) {
            Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
            impresora.setUsuarioActualizacion(usuario);
        }

        impresora = impresoraRepository.save(impresora);

        return toDTO(impresora);
    }

    @Override
    public ImpresoraAndroidDTO cambiarEstado(Long id, Boolean activa, Long usuarioId) {
        ImpresoraAndroid impresora = impresoraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Impresora no encontrada"));

        impresora.setActiva(activa);

        // Auditoría
        if (usuarioId != null) {
            Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
            impresora.setUsuarioActualizacion(usuario);
        }

        impresora = impresoraRepository.save(impresora);

        return toDTO(impresora);
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private void quitarPredeterminada(Long sucursalId, TipoUsoImpresora tipoUso) {
        List<ImpresoraAndroid> impresoras;
        
        if (tipoUso == null) {
            // Buscar todas las predeterminadas sin tipo de uso específico
            impresoras = impresoraRepository.findBySucursalId(sucursalId)
                    .stream()
                    .filter(i -> Boolean.TRUE.equals(i.getPredeterminada()) && i.getTipoUso() == null)
                    .collect(Collectors.toList());
        } else {
            // Buscar predeterminadas del mismo tipo de uso
            impresoras = impresoraRepository.findBySucursalIdAndTipoUso(sucursalId, tipoUso)
                    .stream()
                    .filter(i -> Boolean.TRUE.equals(i.getPredeterminada()))
                    .collect(Collectors.toList());
        }

        impresoras.forEach(i -> i.setPredeterminada(false));
        impresoraRepository.saveAll(impresoras);
    }

    private ImpresoraAndroidDTO toDTO(ImpresoraAndroid entity) {
        ImpresoraAndroidDTO dto = new ImpresoraAndroidDTO();
        dto.setId(entity.getId());
        dto.setSucursalId(entity.getSucursal().getId());
        dto.setNombre(entity.getNombre());
        dto.setTipo(entity.getTipo());
        dto.setIp(entity.getIp());
        dto.setPuerto(entity.getPuerto());
        dto.setAnchoPapel(entity.getAnchoPapel());
        dto.setTipoUso(entity.getTipoUso());
        dto.setPredeterminada(entity.getPredeterminada());
        dto.setActiva(entity.getActiva());
        dto.setFechaCreacion(entity.getFechaCreacion());
        dto.setFechaActualizacion(entity.getFechaActualizacion());
        
        if (entity.getUsuarioCreacion() != null) {
            dto.setUsuarioCreacion(entity.getUsuarioCreacion().getNombre());
        }
        if (entity.getUsuarioActualizacion() != null) {
            dto.setUsuarioActualizacion(entity.getUsuarioActualizacion().getNombre());
        }
        
        return dto;
    }
}