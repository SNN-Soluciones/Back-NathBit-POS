package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.familia.ActualizarFamiliaProductoRequest;
import com.snnsoluciones.backnathbitpos.dto.familia.CrearFamiliaProductoRequest;
import com.snnsoluciones.backnathbitpos.dto.familia.FamiliaProductoDTO;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.FamiliaProducto;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.FamiliaProductoRepository;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
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
 * Soporta familias globales (empresa) y específicas por sucursal
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FamiliaProductoServiceImpl implements FamiliaProductoService {

    private final FamiliaProductoRepository familiaRepository;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public List<FamiliaProductoDTO> listarPorEmpresaYSucursal(Long empresaId, Long sucursalId) {
        log.debug("Listando familias - empresa: {}, sucursal: {}", empresaId, sucursalId);

        Long sucursalIdFinal = (sucursalId == null) ? 0L : sucursalId;
        List<FamiliaProducto> familias = familiaRepository.findByEmpresaAndSucursal(empresaId, sucursalIdFinal);

        return familias.stream()
            .map(this::convertirADto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FamiliaProductoDTO> listarActivasPorEmpresaYSucursal(Long empresaId, Long sucursalId) {
        log.debug("Listando familias activas - empresa: {}, sucursal: {}", empresaId, sucursalId);

        Long sucursalIdFinal = (sucursalId == null) ? 0L : sucursalId;
        List<FamiliaProducto> familias = familiaRepository.findActivasByEmpresaAndSucursal(empresaId, sucursalIdFinal);

        return familias.stream()
            .map(this::convertirADto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FamiliaProductoDTO> buscarPorEmpresaYSucursal(Long empresaId, Long sucursalId, String busqueda) {
        log.debug("Buscando familias - empresa: {}, sucursal: {}, término: {}", empresaId, sucursalId, busqueda);

        if (busqueda == null || busqueda.trim().isEmpty()) {
            return listarPorEmpresaYSucursal(empresaId, sucursalId);
        }

        Long sucursalIdFinal = (sucursalId == null) ? 0L : sucursalId;
        List<FamiliaProducto> familias = familiaRepository.buscarPorEmpresaAndSucursal(
            empresaId, sucursalIdFinal, busqueda.trim());

        return familias.stream()
            .map(this::convertirADto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FamiliaProductoDTO obtenerPorId(Long id, Long empresaId, Long sucursalId) {
        log.debug("Obteniendo familia: {} - empresa: {}, sucursal: {}", id, empresaId, sucursalId);

        Long sucursalIdFinal = (sucursalId == null) ? 0L : sucursalId;
        FamiliaProducto familia = familiaRepository.findByIdAndEmpresaAndSucursal(id, empresaId, sucursalIdFinal)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Familia no encontrada con ID: " + id));

        return convertirADto(familia);
    }

    @Override
    @Transactional
    public FamiliaProductoDTO crear(CrearFamiliaProductoRequest request, Long empresaId, Long sucursalId) {
        log.info("Creando familia - empresa: {}, sucursal: {}", empresaId, sucursalId);

        // Validar empresa
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada: " + empresaId));

        // Validar sucursal si viene
        Sucursal sucursal = null;
        Long sucursalIdFinal = (sucursalId == null || sucursalId == 0) ? 0L : sucursalId;

        if (sucursalIdFinal > 0) {
            sucursal = sucursalRepository.findById(sucursalIdFinal)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + sucursalIdFinal));
        }

        // Validar código único
        if (familiaRepository.existsByCodigoAndEmpresaAndSucursal(
            request.getCodigo(), empresaId, sucursalIdFinal)) {
            throw new BusinessException("Ya existe una familia con el código '" + request.getCodigo() + "'");
        }

        // Crear entidad
        FamiliaProducto familia = FamiliaProducto.builder()
            .empresa(empresa)
            .sucursal(sucursal)
            .nombre(request.getNombre())
            .descripcion(request.getDescripcion())
            .codigo(request.getCodigo().toUpperCase())
            .color(request.getColor())
            .icono(request.getIcono())
            .activa(request.getActiva())
            .orden(request.getOrden())
            .build();

        familia = familiaRepository.save(familia);
        log.info("Familia creada: {} - Global: {}", familia.getId(), sucursal == null);

        return convertirADto(familia);
    }

    @Override
    @Transactional
    public FamiliaProductoDTO actualizar(Long id, ActualizarFamiliaProductoRequest request,
        Long empresaId, Long sucursalId) {
        log.info("Actualizando familia: {} - empresa: {}, sucursal: {}", id, empresaId, sucursalId);

        Long sucursalIdFinal = (sucursalId == null) ? 0L : sucursalId;

        // Buscar familia
        FamiliaProducto familia = familiaRepository.findByIdAndEmpresaAndSucursal(id, empresaId, sucursalIdFinal)
            .orElseThrow(() -> new ResourceNotFoundException("Familia no encontrada: " + id));

        // Validar código único
        if (familiaRepository.existsByCodigoAndEmpresaAndSucursalAndIdNot(
            request.getCodigo(), empresaId, sucursalIdFinal, id)) {
            throw new BusinessException("Ya existe otra familia con el código '" + request.getCodigo() + "'");
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
        log.info("Familia actualizada: {}", familia.getId());

        return convertirADto(familia);
    }

    @Override
    @Transactional
    public void eliminar(Long id, Long empresaId, Long sucursalId) {
        log.info("Eliminando familia: {} - empresa: {}, sucursal: {}", id, empresaId, sucursalId);

        Long sucursalIdFinal = (sucursalId == null) ? 0L : sucursalId;

        FamiliaProducto familia = familiaRepository.findByIdAndEmpresaAndSucursal(id, empresaId, sucursalIdFinal)
            .orElseThrow(() -> new ResourceNotFoundException("Familia no encontrada: " + id));

        familiaRepository.delete(familia);
        log.info("Familia eliminada: {}", id);
    }

    @Override
    @Transactional
    public FamiliaProductoDTO cambiarEstado(Long id, Boolean activa, Long empresaId, Long sucursalId) {
        log.info("Cambiando estado familia: {} a {} - empresa: {}, sucursal: {}",
            id, activa, empresaId, sucursalId);

        Long sucursalIdFinal = (sucursalId == null) ? 0L : sucursalId;

        FamiliaProducto familia = familiaRepository.findByIdAndEmpresaAndSucursal(id, empresaId, sucursalIdFinal)
            .orElseThrow(() -> new ResourceNotFoundException("Familia no encontrada: " + id));

        familia.setActiva(activa);
        familia = familiaRepository.save(familia);

        log.info("Estado familia actualizado: {} - Activa: {}", id, activa);

        return convertirADto(familia);
    }

    /**
     * Convierte entidad a DTO
     */
    private FamiliaProductoDTO convertirADto(FamiliaProducto familia) {
        FamiliaProductoDTO dto = modelMapper.map(familia, FamiliaProductoDTO.class);
        dto.setEmpresaId(familia.getEmpresa().getId());

        if (familia.getSucursal() != null) {
            dto.setSucursalId(familia.getSucursal().getId());
            dto.setSucursalNombre(familia.getSucursal().getNombre());
        }

        dto.setCantidadProductos(0L); // TODO: contar productos en futuro

        return dto;
    }
}