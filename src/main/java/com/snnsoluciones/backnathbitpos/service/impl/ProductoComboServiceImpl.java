// ProductoComboServiceImpl.java
package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.producto.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.TipoInventario;
import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.ProductoComboService;
import com.snnsoluciones.backnathbitpos.service.ProductoInventarioService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoComboServiceImpl implements ProductoComboService {
    
    private final ProductoRepository productoRepository;
    private final ProductoComboRepository comboRepository;
    private final ProductoComboItemRepository comboItemRepository;
    private final ProductoInventarioService inventarioService;
    private final ModelMapper modelMapper;
    
    @Override
    @Transactional
    public ProductoComboDto crear(Long empresaId, Long productoId, ProductoComboRequest request) {
        log.info("Creando combo para producto: {}", productoId);
        
        // Validar producto existe y es de la empresa
        Producto producto = validarProducto(empresaId, productoId);
        
        // Validar que sea tipo COMBO
        if (producto.getTipo() != TipoProducto.COMBO) {
            throw new BusinessException("El producto debe ser tipo COMBO");
        }
        
        // Validar que no exista ya un combo
        if (comboRepository.existsByProductoId(productoId)) {
            throw new BusinessException("El producto ya tiene configuración de combo");
        }
        
        // Crear combo
        ProductoCombo combo = new ProductoCombo();
        combo.setProducto(producto);
        combo.setPrecioCombo(request.getPrecioCombo());
        combo.setDescripcionCombo(request.getDescripcionCombo());
        
        combo = comboRepository.save(combo);
        
        // Crear items del combo
        BigDecimal sumaPrecios = BigDecimal.ZERO;
        int orden = 0;
        
        for (ProductoComboRequest.ComboItemRequest itemRequest : request.getItems()) {
            Producto productoItem = productoRepository.findById(itemRequest.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto del combo no encontrado"));
            
            // Validar que sea de la misma empresa
            if (!productoItem.getEmpresa().getId().equals(empresaId)) {
                throw new BusinessException("El producto " + productoItem.getNombre() + " no pertenece a la empresa");
            }
            
            ProductoComboItem item = new ProductoComboItem();
            item.setCombo(combo);
            item.setProducto(productoItem);
            item.setCantidad(itemRequest.getCantidad());
            item.setPrecioUnitarioReferencia(productoItem.getPrecioBase());
            item.setOrden(itemRequest.getOrden() != null ? itemRequest.getOrden() : orden++);
            
            comboItemRepository.save(item);
            
            // Calcular suma de precios
            sumaPrecios = sumaPrecios.add(
                productoItem.getPrecioBase().multiply(itemRequest.getCantidad())
            );
        }
        
        // Calcular y guardar ahorro
        combo.setAhorro(sumaPrecios.subtract(request.getPrecioCombo()));
        comboRepository.save(combo);
        
        return convertirADto(combo);
    }
    
    @Override
    @Transactional
    public ProductoComboDto actualizar(Long empresaId, Long productoId, ProductoComboRequest request) {
        log.info("Actualizando combo del producto: {}", productoId);
        
        validarProducto(empresaId, productoId);
        
        ProductoCombo combo = comboRepository.findByProductoId(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Combo no encontrado"));
        
        // Actualizar datos básicos
        combo.setPrecioCombo(request.getPrecioCombo());
        combo.setDescripcionCombo(request.getDescripcionCombo());
        
        // Eliminar items anteriores
        comboItemRepository.deleteByComboId(combo.getId());
        
        // Recrear items
        BigDecimal sumaPrecios = BigDecimal.ZERO;
        int orden = 0;
        
        for (ProductoComboRequest.ComboItemRequest itemRequest : request.getItems()) {
            Producto productoItem = productoRepository.findById(itemRequest.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto del combo no encontrado"));
            
            if (!productoItem.getEmpresa().getId().equals(empresaId)) {
                throw new BusinessException("El producto " + productoItem.getNombre() + " no pertenece a la empresa");
            }
            
            ProductoComboItem item = new ProductoComboItem();
            item.setCombo(combo);
            item.setProducto(productoItem);
            item.setCantidad(itemRequest.getCantidad());
            item.setPrecioUnitarioReferencia(productoItem.getPrecioBase());
            item.setOrden(itemRequest.getOrden() != null ? itemRequest.getOrden() : orden++);
            
            comboItemRepository.save(item);
            
            sumaPrecios = sumaPrecios.add(
                productoItem.getPrecioBase().multiply(itemRequest.getCantidad())
            );
        }
        
        combo.setAhorro(sumaPrecios.subtract(request.getPrecioCombo()));
        combo = comboRepository.save(combo);
        
        return convertirADto(combo);
    }
    
    @Override
    @Transactional
    public void eliminar(Long empresaId, Long productoId) {
        log.info("Eliminando combo del producto: {}", productoId);
        
        validarProducto(empresaId, productoId);
        
        ProductoCombo combo = comboRepository.findByProductoId(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Combo no encontrado"));
        
        comboRepository.delete(combo); // Los items se eliminan por CASCADE
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProductoComboDto buscarPorProductoId(Long empresaId, Long productoId) {
        validarProducto(empresaId, productoId);
        
        ProductoCombo combo = comboRepository.findByProductoId(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Combo no encontrado"));
        
        return convertirADto(combo);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductoComboDto> listarPorEmpresa(Long empresaId) {
        return productoRepository
            .findByEmpresaIdAndTipoAndActivoTrue(empresaId, TipoProducto.COMBO)
            .stream()
            .map(producto -> {
                try {
                    return buscarPorProductoId(empresaId, producto.getId());
                } catch (Exception e) {
                    log.warn("Combo sin configuración para producto: {}", producto.getId());
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean esCombo(Long productoId) {
        return comboRepository.existsByProductoId(productoId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean tieneStock(Long productoId, Long sucursalId) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        
        if (producto.getTipoInventario() == TipoInventario.PROPIO) {
            // Combo con stock propio
            try {
                BigDecimal stock = inventarioService.obtenerInventario(productoId, sucursalId)
                    .getCantidadActual();
                return stock.compareTo(BigDecimal.ZERO) > 0;
            } catch (Exception e) {
                return false;
            }
        } else {
            // Combo referencia - verificar stock de cada item
            ProductoCombo combo = comboRepository.findByProductoId(productoId)
                .orElse(null);
            
            if (combo == null) return false;
            
            List<ProductoComboItem> items = comboItemRepository.findByComboIdOrderByOrden(combo.getId());
            
            for (ProductoComboItem item : items) {
                try {
                    BigDecimal stockItem = inventarioService
                        .obtenerInventario(item.getProducto().getId(), sucursalId)
                        .getCantidadActual();
                    
                    if (stockItem.compareTo(item.getCantidad()) < 0) {
                        return false;
                    }
                } catch (Exception e) {
                    return false;
                }
            }
            
            return true;
        }
    }
    
    @Override
    public BigDecimal calcularAhorro(Long productoId) {
        ProductoCombo combo = comboRepository.findByProductoId(productoId)
            .orElse(null);
        
        return combo != null ? combo.getAhorro() : BigDecimal.ZERO;
    }
    
    @Override
    @Transactional
    public void validarDisponibilidad(Long productoId, Long sucursalId, BigDecimal cantidad) {
        if (!tieneStock(productoId, sucursalId)) {
            throw new BusinessException("No hay stock disponible para el combo");
        }
        // Aquí podrías agregar más validaciones según cantidad solicitada
    }
    
    @Override
    @Transactional
    public void descontarInventario(Long productoId, Long sucursalId, BigDecimal cantidad) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        
        ProductoCombo combo = comboRepository.findByProductoId(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Combo no encontrado"));
        
        if (producto.getTipoInventario() == TipoInventario.PROPIO) {
            // Descontar del inventario del combo
            inventarioService.reducirInventario(productoId, sucursalId, cantidad, "Venta de combo");
        }
        
        // Siempre descontar items (sea PROPIO o REFERENCIA)
        List<ProductoComboItem> items = comboItemRepository.findByComboIdOrderByOrden(combo.getId());
        
        for (ProductoComboItem item : items) {
            BigDecimal cantidadADescontar = item.getCantidad().multiply(cantidad);
            inventarioService.reducirInventario(
                item.getProducto().getId(), 
                sucursalId, 
                cantidadADescontar, 
                "Venta como parte del combo: " + producto.getNombre()
            );
        }
    }
    
    // ========== MÉTODOS PRIVADOS ==========
    
    private Producto validarProducto(Long empresaId, Long productoId) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        
        if (!producto.getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException("El producto no pertenece a la empresa");
        }
        
        return producto;
    }
    
    private ProductoComboDto convertirADto(ProductoCombo combo) {
        ProductoComboDto dto = modelMapper.map(combo, ProductoComboDto.class);
        
        // Cargar items
        List<ProductoComboItem> items = comboItemRepository
            .findByComboIdOrderByOrden(combo.getId());
        
        List<ProductoComboItemDto> itemDtos = items.stream()
            .map(item -> {
                ProductoComboItemDto itemDto = modelMapper.map(item, ProductoComboItemDto.class);
                itemDto.setProductoId(item.getProducto().getId());
                itemDto.setProductoNombre(item.getProducto().getNombre());
                itemDto.setProductoCodigo(item.getProducto().getCodigoInterno());
                return itemDto;
            })
            .collect(Collectors.toList());
        
        dto.setItems(itemDtos);
        dto.setProductoId(combo.getProducto().getId());
        
        return dto;
    }
}