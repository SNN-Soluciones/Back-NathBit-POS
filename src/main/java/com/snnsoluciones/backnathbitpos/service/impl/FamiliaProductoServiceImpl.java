package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.familia.ActualizarFamiliaProductoRequest;
import com.snnsoluciones.backnathbitpos.dto.familia.CrearFamiliaProductoRequest;
import com.snnsoluciones.backnathbitpos.dto.familia.FamiliaProductoDTO;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.FamiliaProducto;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.FamiliaProductoRepository;
import com.snnsoluciones.backnathbitpos.service.FamiliaProductoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de gestión de Familias de Productos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FamiliaProductoServiceImpl implements FamiliaProductoService {
    
    private final FamiliaProductoRepository familiaRepository;
    private final EmpresaRepository empresaRepository;
    private final ModelMapper modelMapper;
    
    @Override
    @Transactional(readOnly = true)
    public List<FamiliaProductoDTO> listarPorEmpresa(Long empresaId) {
        log.debug("Listando familias de empresa: {}", empresaId);
        
        List<FamiliaProducto> familias = familiaRepository.findByEmpresaIdOrderByOrdenAsc(empresaId);
        
        return familias.stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<FamiliaProductoDTO> listarActivasPorEmpresa(Long empresaId) {
        log.debug("Listando familias activas de empresa: {}", empresaId);
        
        List<FamiliaProducto> familias = familiaRepository.findByEmpresaIdAndActivaTrueOrderByOrdenAsc(empresaId);
        
        return familias.stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<FamiliaProductoDTO> buscarPorEmpresa(Long empresaId, String busqueda) {
        log.debug("Buscando familias en empresa: {} con término: {}", empresaId, busqueda);
        
        if (busqueda == null || busqueda.trim().isEmpty()) {
            return listarPorEmpresa(empresaId);
        }
        
        List<FamiliaProducto> familias = familiaRepository.buscarPorEmpresa(empresaId, busqueda.trim());
        
        return familias.stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public FamiliaProductoDTO obtenerPorId(Long id, Long empresaId) {
        log.debug("Obteniendo familia: {} de empresa: {}", id, empresaId);
        
        FamiliaProducto familia = familiaRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Familia no encontrada con ID: " + id + " en empresa: " + empresaId));
        
        return convertirADto(familia);
    }
    
    @Override
    @Transactional
    public FamiliaProductoDTO crear(CrearFamiliaProductoRequest request, Long empresaId) {
        log.info("Creando nueva familia para empresa: {}", empresaId);
        
        // Validar que la empresa existe
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada: " + empresaId));
        
        // Validar que el código no existe en la empresa
        if (familiaRepository.existsByCodigoAndEmpresaId(request.getCodigo(), empresaId)) {
            throw new BusinessException("Ya existe una familia con el código '" + request.getCodigo() 
                    + "' en esta empresa");
        }
        
        // Crear entidad
        FamiliaProducto familia = FamiliaProducto.builder()
                .empresa(empresa)
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .codigo(request.getCodigo().toUpperCase()) // Normalizar a mayúsculas
                .color(request.getColor())
                .icono(request.getIcono())
                .activa(request.getActiva())
                .orden(request.getOrden())
                .build();
        
        familia = familiaRepository.save(familia);
        log.info("Familia creada exitosamente con ID: {}", familia.getId());
        
        return convertirADto(familia);
    }
    
    @Override
    @Transactional
    public FamiliaProductoDTO actualizar(Long id, ActualizarFamiliaProductoRequest request, Long empresaId) {
        log.info("Actualizando familia: {} de empresa: {}", id, empresaId);
        
        // Buscar familia existente
        FamiliaProducto familia = familiaRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Familia no encontrada con ID: " + id + " en empresa: " + empresaId));
        
        // Validar código único (excluyendo la familia actual)
        if (familiaRepository.existsByCodigoAndEmpresaIdAndIdNot(request.getCodigo(), empresaId, id)) {
            throw new BusinessException("Ya existe otra familia con el código '" + request.getCodigo() 
                    + "' en esta empresa");
        }
        
        // Actualizar campos
        familia.setNombre(request.getNombre());
        familia.setDescripcion(request.getDescripcion());
        familia.setCodigo(request.getCodigo().toUpperCase());
        familia.setColor(request.getColor());
        familia.setIcono(request.getIcono());
        familia.setActiva(request.getActiva());
        familia.setOrden(request.getOrden());
        
        familia = familiaRepository.save(familia);
        log.info("Familia actualizada exitosamente: {}", familia.getId());
        
        return convertirADto(familia);
    }
    
    @Override
    @Transactional
    public void eliminar(Long id, Long empresaId) {
        log.info("Eliminando familia: {} de empresa: {}", id, empresaId);
        
        FamiliaProducto familia = familiaRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Familia no encontrada con ID: " + id + " en empresa: " + empresaId));
        
        // TODO: Validar que no tenga productos asociados antes de eliminar
        // Esta validación se puede agregar en una fase posterior
        
        familiaRepository.delete(familia);
        log.info("Familia eliminada exitosamente: {}", id);
    }
    
    @Override
    @Transactional
    public FamiliaProductoDTO cambiarEstado(Long id, Boolean activa, Long empresaId) {
        log.info("Cambiando estado de familia: {} a {} en empresa: {}", id, activa, empresaId);
        
        FamiliaProducto familia = familiaRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Familia no encontrada con ID: " + id + " en empresa: " + empresaId));
        
        familia.setActiva(activa);
        familia = familiaRepository.save(familia);
        
        log.info("Estado de familia actualizado: {} - Activa: {}", id, activa);
        
        return convertirADto(familia);
    }
    
    /**
     * Convierte una entidad FamiliaProducto a DTO
     */
    private FamiliaProductoDTO convertirADto(FamiliaProducto familia) {
        FamiliaProductoDTO dto = modelMapper.map(familia, FamiliaProductoDTO.class);
        dto.setEmpresaId(familia.getEmpresa().getId());
        
        // TODO: En el futuro, contar productos de esta familia
        dto.setCantidadProductos(0L);
        
        return dto;
    }
}