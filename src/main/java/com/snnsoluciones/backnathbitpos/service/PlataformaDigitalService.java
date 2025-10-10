package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.plataforma.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.exception.*;
import com.snnsoluciones.backnathbitpos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlataformaDigitalService {

    private final PlataformaDigitalConfigRepository repository;
    private final EmpresaRepository empresaRepository;

    /**
     * Crear nueva plataforma digital
     */
    @Transactional
    public PlataformaDigitalDTO crear(Long empresaId, CrearPlataformaRequest request) {
        log.info("Creando plataforma digital {} para empresa {}", request.getCodigo(), empresaId);

        // Validar empresa
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        // Validar unicidad
        if (repository.existsByEmpresaIdAndCodigo(empresaId, request.getCodigo())) {
            throw new BusinessException("Ya existe una plataforma con código: " + request.getCodigo());
        }

        // Crear entity
        PlataformaDigitalConfig plataforma = PlataformaDigitalConfig.builder()
            .empresa(empresa)
            .codigo(request.getCodigo().toUpperCase())
            .nombre(request.getNombre())
            .porcentajeIncremento(request.getPorcentajeIncremento())
            .colorHex(request.getColorHex())
            .icono(request.getIcono())
            .descripcion(request.getDescripcion())
            .activo(true)
            .orden(obtenerSiguienteOrden(empresaId))
            .build();

        plataforma = repository.save(plataforma);
        log.info("Plataforma digital creada con ID: {}", plataforma.getId());

        return toDTO(plataforma);
    }

    /**
     * Listar plataformas activas por empresa
     */
    @Transactional(readOnly = true)
    public List<PlataformaDigitalDTO> listarActivas(Long empresaId, Long sucursalId) {
        return repository.findActivasByEmpresaIdAndSucursalId(empresaId, sucursalId)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Listar todas por empresa (admin)
     */
    @Transactional(readOnly = true)
    public List<PlataformaDigitalDTO> listarTodas(Long empresaId) {
        return repository.findByEmpresaIdOrderByOrdenAsc(empresaId)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Actualizar plataforma
     */
    @Transactional
    public PlataformaDigitalDTO actualizar(Long id, ActualizarPlataformaRequest request) {
        PlataformaDigitalConfig plataforma = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Plataforma no encontrada"));

        plataforma.setNombre(request.getNombre());
        plataforma.setPorcentajeIncremento(request.getPorcentajeIncremento());
        plataforma.setColorHex(request.getColorHex());
        plataforma.setIcono(request.getIcono());
        plataforma.setDescripcion(request.getDescripcion());

        return toDTO(repository.save(plataforma));
    }

    /**
     * Activar/Desactivar
     */
    @Transactional
    public void cambiarEstado(Long id, Boolean activo) {
        PlataformaDigitalConfig plataforma = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Plataforma no encontrada"));
        
        plataforma.setActivo(activo);
        repository.save(plataforma);
    }

    /**
     * Obtener por ID
     */
    @Transactional(readOnly = true)
    public PlataformaDigitalConfig obtenerPorId(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Plataforma no encontrada"));
    }

    // Mapeo a DTO
    private PlataformaDigitalDTO toDTO(PlataformaDigitalConfig entity) {
        return PlataformaDigitalDTO.builder()
            .id(entity.getId())
            .empresaId(entity.getEmpresa().getId())
            .sucursalId(entity.getSucursal() != null ? entity.getSucursal().getId() : null) // ⭐
            .sucursalNombre(entity.getSucursal() != null ? entity.getSucursal().getNombre() : "Todas") // ⭐
            .codigo(entity.getCodigo())
            .nombre(entity.getNombre())
            .porcentajeIncremento(entity.getPorcentajeIncremento())
            .activo(entity.getActivo())
            .colorHex(entity.getColorHex())
            .icono(entity.getIcono())
            .orden(entity.getOrden())
            .descripcion(entity.getDescripcion())
            .build();
    }

    private Integer obtenerSiguienteOrden(Long empresaId) {
        return repository.findByEmpresaIdOrderByOrdenAsc(empresaId)
            .stream()
            .mapToInt(PlataformaDigitalConfig::getOrden)
            .max()
            .orElse(0) + 1;
    }
}