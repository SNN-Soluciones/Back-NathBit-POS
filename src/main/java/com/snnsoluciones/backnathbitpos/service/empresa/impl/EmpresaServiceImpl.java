package com.snnsoluciones.backnathbitpos.service.empresa.impl;

import com.snnsoluciones.backnathbitpos.dto.empresa.*;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.enums.TipoEmpresa;
import com.snnsoluciones.backnathbitpos.enums.PlanSuscripcion;
import com.snnsoluciones.backnathbitpos.exception.ConflictException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.mapper.EmpresaMapper;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.service.empresa.EmpresaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class EmpresaServiceImpl implements EmpresaService {
    
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final EmpresaMapper empresaMapper;
    
    @Override
    public EmpresaDTO crearEmpresa(CrearEmpresaRequest request) {
        // Validar código único
        if (existePorCodigo(request.getCodigo())) {
            throw new ConflictException("Ya existe una empresa con el código: " + request.getCodigo());
        }
        
        // Validar cédula jurídica única
        if (existePorCedulaJuridica(request.getCedulaJuridica())) {
            throw new ConflictException("Ya existe una empresa con la cédula jurídica: " + 
                request.getCedulaJuridica());
        }
        
        // Crear empresa
        Empresa empresa = new Empresa();
        empresa.setCodigo(request.getCodigo());
        empresa.setNombre(request.getNombre());
        empresa.setNombreComercial(request.getNombreComercial());
        empresa.setCedulaJuridica(request.getCedulaJuridica());
        empresa.setTelefono(request.getTelefono());
        empresa.setEmail(request.getEmail());
        empresa.setDireccion(request.getDireccion());
        empresa.setTipo(request.getTipo() != null ? request.getTipo() : TipoEmpresa.RESTAURANTE);
        empresa.setPlanSuscripcion(request.getPlan() != null ? request.getPlan() : PlanSuscripcion.BASICO);
        empresa.setActiva(true);
        
        // Configuración inicial
        Map<String, Object> configuracion = new HashMap<>();
        configuracion.put("moneda", "CRC");
        configuracion.put("decimales", 2);
        configuracion.put("impuesto", 13);
        configuracion.put("propina_sugerida", 10);
        configuracion.put("permitir_descuentos", true);
        configuracion.put("requiere_autorizacion_descuentos", true);
        configuracion.put("max_descuento_sin_autorizacion", 10);
        empresa.setConfiguracion(configuracion);
        
        empresa = empresaRepository.save(empresa);
        
        // Crear sucursal principal automáticamente
        Sucursal sucursalPrincipal = new Sucursal();
        sucursalPrincipal.setEmpresa(empresa);
        sucursalPrincipal.setCodigo("PRINCIPAL");
        sucursalPrincipal.setNombre("Sucursal Principal");
        sucursalPrincipal.setDireccion(empresa.getDireccion());
        sucursalPrincipal.setTelefono(empresa.getTelefono());
        sucursalPrincipal.setEmail(empresa.getEmail());
        sucursalPrincipal.setEsPrincipal(true);
        sucursalPrincipal.setActiva(true);
        
        sucursalRepository.save(sucursalPrincipal);
        
        log.info("Empresa creada: {} con sucursal principal", empresa.getNombre());
        
        return empresaMapper.toDTO(empresa);
    }
    
    @Override
    @Transactional(readOnly = true)
    public EmpresaDTO obtenerPorId(Long id) {
        Empresa empresa = empresaRepository.findByIdWithSucursales(id)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con id: " + id));
        
        return empresaMapper.toDTOWithSucursales(empresa);
    }
    
    @Override
    @Transactional(readOnly = true)
    public EmpresaDTO obtenerPorCodigo(String codigo) {
        Empresa empresa = empresaRepository.findByCodigo(codigo)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con código: " + codigo));
        
        return empresaMapper.toDTO(empresa);
    }
    
    @Override
    public EmpresaDTO actualizarEmpresa(Long id, EmpresaDTO empresaDTO) {
        Empresa empresa = empresaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        
        // Validar código si cambió
        if (!empresa.getCodigo().equals(empresaDTO.getCodigo()) && 
            existePorCodigo(empresaDTO.getCodigo())) {
            throw new ConflictException("Ya existe una empresa con el código: " + empresaDTO.getCodigo());
        }
        
        // Validar cédula si cambió
        if (!empresa.getCedulaJuridica().equals(empresaDTO.getCedulaJuridica()) &&
            existePorCedulaJuridica(empresaDTO.getCedulaJuridica())) {
            throw new ConflictException("Ya existe una empresa con la cédula jurídica: " + 
                empresaDTO.getCedulaJuridica());
        }
        
        // Actualizar datos
        empresa.setCodigo(empresaDTO.getCodigo());
        empresa.setNombre(empresaDTO.getNombre());
        empresa.setNombreComercial(empresaDTO.getNombreComercial());
        empresa.setCedulaJuridica(empresaDTO.getCedulaJuridica());
        empresa.setTelefono(empresaDTO.getTelefono());
        empresa.setEmail(empresaDTO.getEmail());
        empresa.setDireccion(empresaDTO.getDireccion());
        
        empresa = empresaRepository.save(empresa);
        
        log.info("Empresa actualizada: {}", empresa.getNombre());
        
        return empresaMapper.toDTO(empresa);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<EmpresaDTO> listarEmpresasPorUsuario(Long usuarioId) {
        List<Empresa> empresas = empresaRepository.findByUsuarioId(usuarioId);
        
        return empresas.stream()
            .map(empresaMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<EmpresaDTO> listarEmpresas(Boolean activas, String search, Pageable pageable) {
        Page<Empresa> empresas;
        
        if (activas != null) {
            empresas = empresaRepository.findByActiva(activas, pageable);
        } else if (search != null && !search.isEmpty()) {
            empresas = empresaRepository.buscar(search, null, activas, pageable);
            }
        else {
            empresas = empresaRepository.findAll(pageable);
        }
        
        return empresas.map(empresaMapper::toDTO);
    }
    
    @Override
    public EmpresaDTO cambiarEstadoEmpresa(Long id, boolean activa) {
        Empresa empresa = empresaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        
        empresa.setActiva(activa);
        
        // Si se desactiva la empresa, desactivar también las sucursales
        if (!activa) {
            List<Sucursal> sucursales = sucursalRepository.findByEmpresaIdAndActivaTrue(id);
            sucursales.forEach(s -> s.setActiva(false));
            sucursalRepository.saveAll(sucursales);
        }
        
        empresa = empresaRepository.save(empresa);
        
        log.info("Estado de empresa {} cambiado a: {}", empresa.getNombre(), activa);
        
        return empresaMapper.toDTO(empresa);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ConfiguracionEmpresaDTO obtenerConfiguracion(Long empresaId) {
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        
        ConfiguracionEmpresaDTO config = new ConfiguracionEmpresaDTO();
        config.setEmpresaId(empresaId);
        config.setConfiguracion(empresa.getConfiguracion());
        config.setTipo(empresa.getTipo());
        config.setPlan(empresa.getPlanSuscripcion());
        config.setLimiteUsuarios(empresa.getLimiteUsuarios());
        config.setLimiteSucursales(empresa.getLimiteSucursales());
        
        return config;
    }
    
    @Override
    public ConfiguracionEmpresaDTO actualizarConfiguracion(Long empresaId, 
                                                          ConfiguracionEmpresaDTO configuracion) {
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        
        // Actualizar configuración
        if (configuracion.getConfiguracion() != null) {
            empresa.setConfiguracion(configuracion.getConfiguracion());
        }
        
        if (configuracion.getTipo() != null) {
            empresa.setTipo(configuracion.getTipo());
        }
        
        if (configuracion.getPlan() != null) {
            empresa.getPlanSuscripcion().name();
        }
        
        if (configuracion.getLimiteUsuarios() != null) {
            empresa.setLimiteUsuarios(configuracion.getLimiteUsuarios());
        }
        
        if (configuracion.getLimiteSucursales() != null) {
            empresa.setLimiteSucursales(configuracion.getLimiteSucursales());
        }
        
        empresa = empresaRepository.save(empresa);
        
        log.info("Configuración actualizada para empresa: {}", empresa.getNombre());
        
        return obtenerConfiguracion(empresaId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existePorCodigo(String codigo) {
        return empresaRepository.existsByCodigo(codigo);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existePorCedulaJuridica(String cedulaJuridica) {
        return empresaRepository.existsByCedulaJuridica(cedulaJuridica);
    }
    
    @Override
    @Transactional(readOnly = true)
    public EstadisticasEmpresaDTO obtenerEstadisticas(Long empresaId) {
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        
        EstadisticasEmpresaDTO estadisticas = new EstadisticasEmpresaDTO();
        estadisticas.setEmpresaId(empresaId);
        estadisticas.setNombreEmpresa(empresa.getNombre());
        
        // Contar sucursales activas
        long sucursalesActivas = sucursalRepository.countByEmpresaIdAndActivaTrue(empresaId);
        estadisticas.setSucursalesActivas(sucursalesActivas);
        
        // Otras métricas básicas
        estadisticas.setPlan(empresa.getPlanSuscripcion());
        estadisticas.setLimiteUsuarios(empresa.getLimiteUsuarios());
        estadisticas.setLimiteSucursales(empresa.getLimiteSucursales());
        
        return estadisticas;
    }

    @Override
    public boolean usuarioTieneAcceso(Long usuarioId, Long empresaId) {
        return false;
    }
}