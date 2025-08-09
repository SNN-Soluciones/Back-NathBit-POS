package com.snnsoluciones.backnathbitpos.service.empresa.impl;

import com.snnsoluciones.backnathbitpos.dto.empresa.ConfiguracionSucursalDTO;
import com.snnsoluciones.backnathbitpos.dto.empresa.CrearSucursalRequest;
import com.snnsoluciones.backnathbitpos.dto.empresa.EstadisticasSucursalDTO;
import com.snnsoluciones.backnathbitpos.dto.empresa.SucursalDTO;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.exception.ConflictException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.mapper.SucursalMapper;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.service.empresa.SucursalService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SucursalServiceImpl implements SucursalService {
    
    private final SucursalRepository sucursalRepository;
    private final EmpresaRepository empresaRepository;
    private final SucursalMapper sucursalMapper;
    
    @Override
    public SucursalDTO crearSucursal(Long empresaId, CrearSucursalRequest request)
        throws BadRequestException {
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        
        // Validar límite de sucursales
        long sucursalesActuales = sucursalRepository.countByEmpresaIdAndActivaTrue(empresaId);
        if (empresa.getLimiteSucursales() != null && sucursalesActuales >= empresa.getLimiteSucursales()) {
            throw new BadRequestException("Se ha alcanzado el límite de sucursales para esta empresa");
        }
        
        // Validar código único dentro de la empresa
        if (existePorCodigoYEmpresa(request.getCodigo(), empresaId)) {
            throw new ConflictException("Ya existe una sucursal con el código: " + request.getCodigo());
        }
        
        // Crear sucursal
        Sucursal sucursal = new Sucursal();
        sucursal.setEmpresa(empresa);
        sucursal.setCodigo(request.getCodigo());
        sucursal.setNombre(request.getNombre());
        sucursal.setDireccion(request.getDireccion());
        sucursal.setTelefono(request.getTelefono());
        sucursal.setEmail(request.getEmail());
        sucursal.setEsPrincipal(false);
        sucursal.setActiva(true);
        
        // Configuración inicial
        Map<String, Object> configuracion = new HashMap<>();
        configuracion.put("horario_apertura", "06:00");
        configuracion.put("horario_cierre", "22:00");
        configuracion.put("dias_operacion", List.of("LUN", "MAR", "MIE", "JUE", "VIE", "SAB", "DOM"));
        configuracion.put("permite_delivery", false);
        configuracion.put("permite_takeaway", true);
        configuracion.put("tiempo_preparacion_default", 20);
        sucursal.setConfiguracion(configuracion);
        
        sucursal = sucursalRepository.save(sucursal);
        
        log.info("Sucursal creada: {} para empresa {}", sucursal.getNombre(), empresa.getNombre());
        
        return sucursalMapper.toDTO(sucursal);
    }
    
    @Override
    @Transactional(readOnly = true)
    public SucursalDTO obtenerPorId(Long id) {
        Sucursal sucursal = sucursalRepository.findByIdWithEmpresa(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con id: " + id));
        
        return sucursalMapper.toDTOWithEmpresa(sucursal);
    }
    
    @Override
    public SucursalDTO actualizarSucursal(Long id, SucursalDTO sucursalDTO) {
        Sucursal sucursal = sucursalRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
        
        // Validar código si cambió
        if (!sucursal.getCodigo().equals(sucursalDTO.getCodigo()) &&
            existePorCodigoYEmpresa(sucursalDTO.getCodigo(), sucursal.getEmpresa().getId())) {
            throw new ConflictException("Ya existe una sucursal con el código: " + sucursalDTO.getCodigo());
        }
        
        // Actualizar datos
        sucursal.setCodigo(sucursalDTO.getCodigo());
        sucursal.setNombre(sucursalDTO.getNombre());
        sucursal.setDireccion(sucursalDTO.getDireccion());
        sucursal.setTelefono(sucursalDTO.getTelefono());
        sucursal.setEmail(sucursalDTO.getEmail());
        
        sucursal = sucursalRepository.save(sucursal);
        
        log.info("Sucursal actualizada: {}", sucursal.getNombre());
        
        return sucursalMapper.toDTO(sucursal);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SucursalDTO> listarSucursalesPorEmpresa(Long empresaId, Boolean activas) {
        List<Sucursal> sucursales;
        
        if (activas != null && activas) {
            sucursales = sucursalRepository.findByEmpresaIdAndActivaTrue(empresaId);
        } else {
            sucursales = sucursalRepository.findByEmpresaId(empresaId, Pageable.unpaged()).getContent();
        }
        
        return sucursales.stream()
            .map(sucursalMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SucursalDTO> listarSucursalesPorUsuario(Long usuarioId, Long empresaId) {
        List<Sucursal> sucursales;
        
        if (empresaId != null) {
            sucursales = sucursalRepository.findByUsuarioIdAndEmpresaId(usuarioId, empresaId);
        } else {
            sucursales = sucursalRepository.findByUsuarioId(usuarioId);
        }
        
        return sucursales.stream()
            .map(sucursalMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SucursalDTO> listarSucursales(Long empresaId, String search, Pageable pageable) {
        // Buscar sucursales, por defecto solo las activas
        Page<Sucursal> sucursales = sucursalRepository.buscarPorEmpresa(
            empresaId,
            search,     // búsqueda (puede ser null o vacío)
            true,       // solo activas
            pageable
        );

        return sucursales.map(sucursalMapper::toDTO);
    }
    
    @Override
    public SucursalDTO cambiarEstadoSucursal(Long id, boolean activa) throws BadRequestException {
        Sucursal sucursal = sucursalRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
        
        // No permitir desactivar la sucursal principal
        if (!activa && sucursal.getEsPrincipal()) {
            throw new BadRequestException("No se puede desactivar la sucursal principal");
        }
        
        sucursal.setActiva(activa);
        sucursal = sucursalRepository.save(sucursal);
        
        log.info("Estado de sucursal {} cambiado a: {}", sucursal.getNombre(), activa);
        
        return sucursalMapper.toDTO(sucursal);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ConfiguracionSucursalDTO obtenerConfiguracion(Long sucursalId) {
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
        
        ConfiguracionSucursalDTO config = new ConfiguracionSucursalDTO();
        config.setSucursalId(sucursalId);
        config.setConfiguracion(sucursal.getConfiguracion());
        config.setEsPrincipal(sucursal.getEsPrincipal());
        
        return config;
    }
    
    @Override
    public ConfiguracionSucursalDTO actualizarConfiguracion(Long sucursalId,
                                                           ConfiguracionSucursalDTO configuracion) {
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
        
        if (configuracion.getConfiguracion() != null) {
            sucursal.setConfiguracion(configuracion.getConfiguracion());
        }
        
        sucursal = sucursalRepository.save(sucursal);
        
        log.info("Configuración actualizada para sucursal: {}", sucursal.getNombre());
        
        return obtenerConfiguracion(sucursalId);
    }
    
    @Override
    public void establecerComoPrincipal(Long sucursalId) {
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
        
        // Desmarcar la actual principal
        Sucursal sucursalesPrincipales = sucursalRepository
            .findSucursalPrincipalByEmpresaId(sucursal.getEmpresa().getId()).orElse(null);
        
        if (Objects.nonNull(sucursalesPrincipales) && !sucursalesPrincipales.getId().equals(sucursalId)) {
            sucursalesPrincipales.setEsPrincipal(false);
            sucursalRepository.save(sucursalesPrincipales);
        }
        
        // Marcar la nueva principal
        sucursal.setEsPrincipal(true);
        sucursalRepository.save(sucursal);
        
        log.info("Sucursal {} establecida como principal", sucursal.getNombre());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existePorCodigoYEmpresa(String codigo, Long empresaId) {
        return sucursalRepository.existsByCodigoAndEmpresaId(codigo, empresaId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public EstadisticasSucursalDTO obtenerEstadisticas(Long sucursalId) {
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
        
        EstadisticasSucursalDTO estadisticas = new EstadisticasSucursalDTO();
        estadisticas.setSucursalId(sucursalId);
        estadisticas.setNombreSucursal(sucursal.getNombre());
        estadisticas.setEmpresaId(sucursal.getEmpresa().getId());
        estadisticas.setNombreEmpresa(sucursal.getEmpresa().getNombre());
        
        // Otras métricas básicas que se agregarán más adelante
        estadisticas.setVentasDelDia(0.0);
        estadisticas.setOrdenesActivas(0L);
        estadisticas.setMesasOcupadas(0L);
        
        return estadisticas;
    }
    
    @Override
    public void copiarConfiguracion(Long sucursalOrigenId, Long sucursalDestinoId)
        throws BadRequestException {
        Sucursal origen = sucursalRepository.findById(sucursalOrigenId)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal origen no encontrada"));
        
        Sucursal destino = sucursalRepository.findById(sucursalDestinoId)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal destino no encontrada"));
        
        // Verificar que sean de la misma empresa
        if (!origen.getEmpresa().getId().equals(destino.getEmpresa().getId())) {
            throw new BadRequestException("Las sucursales deben pertenecer a la misma empresa");
        }
        
        // Copiar configuración
        destino.setConfiguracion(new HashMap<>(origen.getConfiguracion()));
        sucursalRepository.save(destino);
        
        log.info("Configuración copiada de sucursal {} a {}", 
                origen.getNombre(), destino.getNombre());
    }
}