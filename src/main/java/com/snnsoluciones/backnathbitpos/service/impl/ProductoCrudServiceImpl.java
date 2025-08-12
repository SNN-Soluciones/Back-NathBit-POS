package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.producto.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.ProductoCrudService;
import com.snnsoluciones.backnathbitpos.service.ProductoValidacionService;
import com.snnsoluciones.backnathbitpos.service.ProductoCategoriaService;
import com.snnsoluciones.backnathbitpos.service.ProductoImpuestoService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoCrudServiceImpl implements ProductoCrudService {
    
    private final ProductoRepository productoRepository;
    private final EmpresaRepository empresaRepository;
    private final EmpresaCABySRepository empresaCABySRepository;
    private final ProductoValidacionService validacionService;
    private final ProductoCategoriaService categoriaService;
    private final ProductoImpuestoService impuestoService;
    private final ModelMapper modelMapper;
    
    @Override
    @Transactional
    public ProductoDto crear(Long empresaId, ProductoCreateDto dto) {
        log.debug("Creando producto: {} para empresa: {}", dto.getNombre(), empresaId);
        
        // Validar empresa
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada: " + empresaId));
        
        // Validar duplicados
        if (validacionService.existeCodigoInterno(dto.getCodigoInterno(), empresaId, null)) {
            throw new BusinessException("Ya existe un producto con el código: " + dto.getCodigoInterno());
        }
        
        if (dto.getCodigoBarras() != null && !dto.getCodigoBarras().isEmpty() &&
            validacionService.existeCodigoBarras(dto.getCodigoBarras(), empresaId, null)) {
            throw new BusinessException("Ya existe un producto con el código de barras: " + dto.getCodigoBarras());
        }
        
        if (validacionService.existeNombre(dto.getNombre(), empresaId, null)) {
            throw new BusinessException("Ya existe un producto con el nombre: " + dto.getNombre());
        }
        
        // Crear producto
        Producto producto = new Producto();
        producto.setEmpresa(empresa);
        producto.setCodigoInterno(dto.getCodigoInterno() != null ? 
            dto.getCodigoInterno() : generarCodigoInterno(empresaId));
        producto.setCodigoBarras(dto.getCodigoBarras());
        producto.setNombre(dto.getNombre());
        producto.setDescripcion(dto.getDescripcion());
        producto.setUnidadMedida(dto.getUnidadMedida());
        producto.setMoneda(dto.getMoneda());
        producto.setPrecioVenta(dto.getPrecioVenta());
        producto.setAplicaServicio(dto.getAplicaServicio() != null ? dto.getAplicaServicio() : false);
        producto.setActivo(true);
        
        // Asignar CABYS
        if (dto.getEmpresaCabysId() != null) {
            EmpresaCAByS cabys = empresaCABySRepository.findById(dto.getEmpresaCabysId())
                .orElseThrow(() -> new ResourceNotFoundException("Código CABYS no encontrado: " + dto.getEmpresaCabysId()));
            producto.setEmpresaCabys(cabys);
        }
        
        // Guardar producto
        producto = productoRepository.save(producto);
        log.info("Producto creado con ID: {}", producto.getId());
        
        // Asignar categorías si vienen
        if (dto.getCategoriaProductoDtos() != null && !dto.getCategoriaProductoDtos().isEmpty()) {
            Set<Long> categoriaIds = dto.getCategoriaProductoDtos().stream()
                .map(CategoriaProductoDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            if (!categoriaIds.isEmpty()) {
                categoriaService.asignarCategorias(producto.getId(), categoriaIds);
            }
        }
        
        // Crear impuestos si vienen
        if (dto.getImpuestos() != null && !dto.getImpuestos().isEmpty()) {
            impuestoService.actualizarImpuestos(producto.getId(), 
                dto.getImpuestos().stream()
                    .map(imp -> modelMapper.map(imp, ProductoImpuestoDto.class))
                    .collect(Collectors.toList())
            );
        }
        
        return convertirADto(producto);
    }
    
    @Override
    @Transactional
    public ProductoDto actualizar(Long empresaId, Long productoId, ProductoUpdateDto dto) {
        log.debug("Actualizando producto: {} de empresa: {}", productoId, empresaId);
        
        // Validar que existe y pertenece a la empresa
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
        
        if (!producto.getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException("El producto no pertenece a la empresa especificada");
        }
        
        // Validar duplicados
        if (!producto.getCodigoInterno().equals(dto.getCodigoInterno()) &&
            validacionService.existeCodigoInterno(dto.getCodigoInterno(), empresaId, productoId)) {
            throw new BusinessException("Ya existe un producto con el código: " + dto.getCodigoInterno());
        }
        
        if (dto.getCodigoBarras() != null && !dto.getCodigoBarras().equals(producto.getCodigoBarras()) &&
            validacionService.existeCodigoBarras(dto.getCodigoBarras(), empresaId, productoId)) {
            throw new BusinessException("Ya existe un producto con el código de barras: " + dto.getCodigoBarras());
        }
        
        if (!producto.getNombre().equals(dto.getNombre()) &&
            validacionService.existeNombre(dto.getNombre(), empresaId, productoId)) {
            throw new BusinessException("Ya existe un producto con el nombre: " + dto.getNombre());
        }
        
        // Actualizar campos
        producto.setCodigoInterno(dto.getCodigoInterno());
        producto.setCodigoBarras(dto.getCodigoBarras());
        producto.setNombre(dto.getNombre());
        producto.setDescripcion(dto.getDescripcion());
        producto.setUnidadMedida(dto.getUnidadMedida());
        producto.setMoneda(dto.getMoneda());
        producto.setPrecioVenta(dto.getPrecioVenta());
        producto.setAplicaServicio(dto.getAplicaServicio());
        producto.setActivo(dto.getActivo());
        
        // Actualizar CABYS si cambió
        if (dto.getEmpresaCabysId() != null && 
            (producto.getEmpresaCabys() == null || !producto.getEmpresaCabys().getId().equals(dto.getEmpresaCabysId()))) {
            EmpresaCAByS cabys = empresaCABySRepository.findById(dto.getEmpresaCabysId())
                .orElseThrow(() -> new ResourceNotFoundException("Código CABYS no encontrado: " + dto.getEmpresaCabysId()));
            producto.setEmpresaCabys(cabys);
        }
        
        producto = productoRepository.save(producto);
        log.info("Producto actualizado ID: {}", producto.getId());
        
        // Actualizar categorías si vienen
        if (dto.getEmpresaCabysId() != null) {
            EmpresaCAByS cabys = empresaCABySRepository.findById(dto.getEmpresaCabysId())
                .orElseThrow(() -> new ResourceNotFoundException("Código CABYS no encontrado: " + dto.getEmpresaCabysId()));
            producto.setEmpresaCabys(cabys);
        }
        
        return convertirADto(producto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProductoDto obtenerPorId(Long empresaId, Long productoId) {
        Producto producto = productoRepository.findByIdConRelaciones(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
        
        if (!producto.getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException("El producto no pertenece a la empresa especificada");
        }
        
        return convertirADto(producto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Producto obtenerEntidadPorId(Long productoId) {
        return productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
    }
    
    @Override
    @Transactional
    public void eliminar(Long empresaId, Long productoId) {
        log.debug("Eliminando producto: {} de empresa: {}", productoId, empresaId);
        
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
        
        if (!producto.getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException("El producto no pertenece a la empresa especificada");
        }
        
        // TODO: Verificar si está en uso en ventas antes de eliminar
        
        productoRepository.deleteById(productoId);
        log.info("Producto eliminado ID: {}", productoId);
    }
    
    @Override
    @Transactional
    public void activarDesactivar(Long empresaId, Long productoId, boolean activo) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
        
        if (!producto.getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException("El producto no pertenece a la empresa especificada");
        }
        
        producto.setActivo(activo);
        productoRepository.save(producto);
        
        log.info("Producto {} {}", productoId, activo ? "activado" : "desactivado");
    }
    
    @Override
    public String generarCodigoInterno(Long empresaId) {
        long count = productoRepository.countByEmpresaIdAndActivoTrue(empresaId);
        String codigo;
        int intentos = 0;
        
        do {
            codigo = String.format("PROD%05d", count + 1 + intentos);
            intentos++;
        } while (validacionService.existeCodigoInterno(codigo, empresaId, null) && intentos < 100);
        
        if (intentos >= 100) {
            throw new BusinessException("No se pudo generar un código único");
        }
        
        return codigo;
    }
    
    // Método auxiliar para convertir a DTO
    private ProductoDto convertirADto(Producto producto) {
        ProductoDto dto = modelMapper.map(producto, ProductoDto.class);
        
        // Mapeos adicionales que ModelMapper no puede hacer automáticamente
        dto.setEmpresaId(producto.getEmpresa().getId());
        
        // Cargar categorías
        Set<Long> categoriaIds = categoriaService.obtenerCategoriaIds(producto.getId());
        // Cargar categorías
        Set<CategoriaProductoDto> categorias = producto.getCategorias().stream()
            .map(cat -> modelMapper.map(cat, CategoriaProductoDto.class))
            .collect(Collectors.toSet());
        dto.setCategoriaProductoDtos(categorias);
        
        // Cargar impuestos
        dto.setImpuestos(
            impuestoService.obtenerImpuestos(producto.getId()).stream()
                .map(imp -> modelMapper.map(imp, ProductoImpuestoDto.class))
                .collect(Collectors.toList())
        );
        
        return dto;
    }
}