package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.producto.ProductoProveedorCreateDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoProveedorDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoProveedorUpdateDto;
import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.entity.ProductoCodigoProveedor;
import com.snnsoluciones.backnathbitpos.entity.Proveedor;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.mappers.ProductoProveedorMapper;
import com.snnsoluciones.backnathbitpos.repository.ProductoCodigoProveedorRepository;
import com.snnsoluciones.backnathbitpos.repository.ProductoRepository;
import com.snnsoluciones.backnathbitpos.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductoProveedorService {

    private final ProductoCodigoProveedorRepository productoProveedorRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoProveedorMapper mapper;

    /**
     * Obtener todos los proveedores de un producto
     */
    @Transactional(readOnly = true)
    public List<ProductoProveedorDto> obtenerProveedoresProducto(Long productoId) {
        log.info("Obteniendo proveedores del producto: {}", productoId);
        
        // Verificar que el producto existe
        if (!productoRepository.existsById(productoId)) {
            throw new ResourceNotFoundException("Producto no encontrado con ID: " + productoId);
        }
        
        return productoProveedorRepository.findByProductoId(productoId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Asociar un proveedor a un producto
     */
    public ProductoProveedorDto asociarProveedor(ProductoProveedorCreateDto dto) {
        log.info("Asociando proveedor {} al producto {}", dto.getProveedorId(), dto.getProductoId());
        
        // Validar que no exista ya la relación
        if (productoProveedorRepository.existsByProductoIdAndProveedorIdAndActivo(
                dto.getProductoId(), dto.getProveedorId(), true)) {
            throw new BusinessException("El proveedor ya está asociado a este producto");
        }
        
        // Validar que no exista el código para ese proveedor
        if (productoProveedorRepository.existsByProveedorIdAndCodigo(
                dto.getProveedorId(), dto.getCodigoProveedor())) {
            throw new BusinessException("El código ya existe para este proveedor");
        }
        
        // Obtener entidades
        Producto producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        
        Proveedor proveedor = proveedorRepository.findById(dto.getProveedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
        
        // Crear la relación
        ProductoCodigoProveedor relacion = new ProductoCodigoProveedor();
        relacion.setProducto(producto);
        relacion.setProveedor(proveedor);
        relacion.setCodigo(dto.getCodigoProveedor());
        relacion.setDescripcionProveedor(dto.getDescripcionProveedor());
        relacion.setUnidadCompra(dto.getUnidadCompra());
        relacion.setFactorConversion(dto.getFactorConversion() != null ? dto.getFactorConversion() : 1);
        relacion.setPrecioCompra(dto.getPrecioCompra());
        relacion.setObservaciones(dto.getObservaciones());
        relacion.setActivo(true);
        
        relacion = productoProveedorRepository.save(relacion);
        
        log.info("Relación creada exitosamente con ID: {}", relacion.getId());
        return mapper.toDto(relacion);
    }

    /**
     * Actualizar relación producto-proveedor
     */
    public ProductoProveedorDto actualizar(Long id, ProductoProveedorUpdateDto dto) {
        log.info("Actualizando relación producto-proveedor: {}", id);
        
        ProductoCodigoProveedor relacion = productoProveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Relación no encontrada"));
        
        // Validar si cambió el código
        if (dto.getCodigoProveedor() != null && !dto.getCodigoProveedor().equals(relacion.getCodigo())) {
            // Verificar que el nuevo código no exista
            if (productoProveedorRepository.existsByProveedorIdAndCodigoAndIdNot(
                    relacion.getProveedor().getId(), dto.getCodigoProveedor(), id)) {
                throw new BusinessException("El código ya existe para este proveedor");
            }
            relacion.setCodigo(dto.getCodigoProveedor());
        }
        
        // Actualizar campos opcionales
        if (dto.getDescripcionProveedor() != null) {
            relacion.setDescripcionProveedor(dto.getDescripcionProveedor());
        }
        if (dto.getUnidadCompra() != null) {
            relacion.setUnidadCompra(dto.getUnidadCompra());
        }
        if (dto.getFactorConversion() != null) {
            relacion.setFactorConversion(dto.getFactorConversion());
        }
        if (dto.getPrecioCompra() != null) {
            relacion.setPrecioCompra(dto.getPrecioCompra());
        }
        if (dto.getObservaciones() != null) {
            relacion.setObservaciones(dto.getObservaciones());
        }
        
        relacion = productoProveedorRepository.save(relacion);
        
        log.info("Relación actualizada exitosamente");
        return mapper.toDto(relacion);
    }

    /**
     * Activar/Desactivar relación
     */
    public void toggleEstado(Long id) {
        log.info("Cambiando estado de relación: {}", id);
        
        ProductoCodigoProveedor relacion = productoProveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Relación no encontrada"));
        
        relacion.setActivo(!relacion.getActivo());
        productoProveedorRepository.save(relacion);
        
        log.info("Estado cambiado a: {}", relacion.getActivo());
    }

    /**
     * Buscar producto por código de proveedor
     */
    @Transactional(readOnly = true)
    public ProductoProveedorDto buscarPorCodigoProveedor(Long proveedorId, String codigo) {
        log.info("Buscando producto con código {} del proveedor {}", codigo, proveedorId);
        
        ProductoCodigoProveedor relacion = productoProveedorRepository
                .findByProveedorIdAndCodigo(proveedorId, codigo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró producto con ese código para el proveedor"));
        
        return mapper.toDto(relacion);
    }

    /**
     * Eliminar relación (soft delete)
     */
    public void eliminar(Long id) {
        log.info("Eliminando relación: {}", id);
        
        ProductoCodigoProveedor relacion = productoProveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Relación no encontrada"));
        
        relacion.setActivo(false);
        productoProveedorRepository.save(relacion);
        
        log.info("Relación eliminada (soft delete)");
    }

    /**
     * Obtener productos de un proveedor
     */
    @Transactional(readOnly = true)
    public List<ProductoProveedorDto> obtenerProductosProveedor(Long proveedorId) {
        log.info("Obteniendo productos del proveedor: {}", proveedorId);
        
        return productoProveedorRepository.findByProveedorIdAndActivo(proveedorId, true).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
}