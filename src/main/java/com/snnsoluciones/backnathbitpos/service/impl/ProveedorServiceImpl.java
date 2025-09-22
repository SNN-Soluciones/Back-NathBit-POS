package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.proveedor.ProveedorDto;
import com.snnsoluciones.backnathbitpos.dto.proveedor.ProveedorRequest;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Proveedor;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.ProveedorRepository;
import com.snnsoluciones.backnathbitpos.service.ProveedorService;
import com.snnsoluciones.backnathbitpos.service.impl.ModularHelperService.QueryParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProveedorServiceImpl implements ProveedorService {
    
    private final ProveedorRepository proveedorRepository;
    private final EmpresaRepository empresaRepository;
    private final ModularHelperService modularHelper;

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorDto> listarPorEmpresa(Long empresaId, String busqueda) {
        List<Proveedor> proveedores;
        
        if (busqueda != null && !busqueda.trim().isEmpty()) {
            proveedores = proveedorRepository.buscarPorEmpresaYTermino(empresaId, busqueda);
        } else {
            proveedores = proveedorRepository.findByEmpresaIdAndActivoTrueOrderByNombreComercialAsc(empresaId);
        }
        
        return proveedores.stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProveedorDto obtenerPorId(Long id) {
        return proveedorRepository.findById(id)
                .map(this::convertirADto)
                .orElse(null);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProveedorDto buscarPorIdentificacion(Long empresaId, String numeroIdentificacion) {
        return proveedorRepository.findByEmpresaIdAndNumeroIdentificacion(empresaId, numeroIdentificacion)
                .map(this::convertirADto)
                .orElse(null);
    }
    
    @Override
    public ProveedorDto crear(ProveedorRequest request) {
        // Verificar si ya existe
        if (proveedorRepository.existsByEmpresaIdAndNumeroIdentificacion(
                request.getEmpresaId(), request.getNumeroIdentificacion())) {
            throw new RuntimeException("Ya existe un proveedor con esa identificación");
        }
        
        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        Long sucursalId = request.getSucursalId() != null ? request.getSucursalId() : null;

        Sucursal sucursal = modularHelper.determinarSucursalParaEntidad(empresa.getId(), sucursalId, "proveedor");

        Proveedor proveedor = Proveedor.builder()
                .empresa(empresa)
                .sucursal(sucursal)
                .tipoIdentificacion(request.getTipoIdentificacion())
                .numeroIdentificacion(request.getNumeroIdentificacion())
                .nombreComercial(request.getNombreComercial())
                .razonSocial(request.getRazonSocial())
                .telefono(request.getTelefono())
                .email(request.getEmail())
                .direccion(request.getDireccion())
                .diasCredito(request.getDiasCredito() != null ? request.getDiasCredito() : 0)
                .contactoNombre(request.getContactoNombre())
                .contactoTelefono(request.getContactoTelefono())
                .notas(request.getNotas())
                .activo(true)
                .build();
        
        proveedor = proveedorRepository.save(proveedor);
        log.info("Proveedor creado: {} - {}", proveedor.getId(), proveedor.getNombreComercial());
        
        return convertirADto(proveedor);
    }
    
    @Override
    public ProveedorDto actualizar(Long id, ProveedorRequest request) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
        
        // Verificar identificación duplicada si cambió
        if (!proveedor.getNumeroIdentificacion().equals(request.getNumeroIdentificacion())) {
            if (proveedorRepository.existsByEmpresaIdAndNumeroIdentificacionAndIdNot(
                    proveedor.getEmpresa().getId(), request.getNumeroIdentificacion(), id)) {
                throw new RuntimeException("Ya existe otro proveedor con esa identificación");
            }
        }
        
        // Actualizar campos
        proveedor.setTipoIdentificacion(request.getTipoIdentificacion());
        proveedor.setNumeroIdentificacion(request.getNumeroIdentificacion());
        proveedor.setNombreComercial(request.getNombreComercial());
        proveedor.setRazonSocial(request.getRazonSocial());
        proveedor.setTelefono(request.getTelefono());
        proveedor.setEmail(request.getEmail());
        proveedor.setDireccion(request.getDireccion());
        proveedor.setDiasCredito(request.getDiasCredito() != null ? request.getDiasCredito() : 0);
        proveedor.setContactoNombre(request.getContactoNombre());
        proveedor.setContactoTelefono(request.getContactoTelefono());
        proveedor.setNotas(request.getNotas());
        
        proveedor = proveedorRepository.save(proveedor);
        log.info("Proveedor actualizado: {}", id);
        
        return convertirADto(proveedor);
    }
    
    @Override
    public ProveedorDto toggleActivo(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
        
        proveedor.setActivo(!proveedor.getActivo());
        proveedor = proveedorRepository.save(proveedor);
        
        log.info("Proveedor {} {}", id, proveedor.getActivo() ? "activado" : "desactivado");
        return convertirADto(proveedor);
    }
    
    @Override
    public void eliminar(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
        
        // Soft delete
        proveedor.setActivo(false);
        proveedorRepository.save(proveedor);
        
        log.info("Proveedor eliminado (soft): {}", id);
    }
    
    // Método auxiliar de conversión
    private ProveedorDto convertirADto(Proveedor proveedor) {
        return ProveedorDto.builder()
                .id(proveedor.getId())
                .empresaId(proveedor.getEmpresa().getId())
                .empresaNombre(proveedor.getEmpresa().getNombreComercial())
                .tipoIdentificacion(proveedor.getTipoIdentificacion())
                .numeroIdentificacion(proveedor.getNumeroIdentificacion())
                .nombreComercial(proveedor.getNombreComercial())
                .razonSocial(proveedor.getRazonSocial())
                .telefono(proveedor.getTelefono())
                .email(proveedor.getEmail())
                .direccion(proveedor.getDireccion())
                .diasCredito(proveedor.getDiasCredito())
                .contactoNombre(proveedor.getContactoNombre())
                .contactoTelefono(proveedor.getContactoTelefono())
                .notas(proveedor.getNotas())
                .activo(proveedor.getActivo())
                .createdAt(proveedor.getCreatedAt())
                .updatedAt(proveedor.getUpdatedAt())
                .build();
    }
}