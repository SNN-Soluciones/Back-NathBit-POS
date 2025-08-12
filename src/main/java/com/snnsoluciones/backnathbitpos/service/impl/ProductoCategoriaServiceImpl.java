package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.CategoriaProducto;
import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.CategoriaProductoRepository;
import com.snnsoluciones.backnathbitpos.repository.ProductoRepository;
import com.snnsoluciones.backnathbitpos.service.ProductoCategoriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoCategoriaServiceImpl implements ProductoCategoriaService {
    
    private final ProductoRepository productoRepository;
    private final CategoriaProductoRepository categoriaRepository;
    
    @Override
    @Transactional
    public void asignarCategorias(Long productoId, Set<Long> categoriaIds) {
        log.debug("Asignando {} categorías al producto {}", categoriaIds.size(), productoId);
        
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
        
        // Limpiar categorías actuales
        producto.getCategorias().clear();
        
        // Asignar nuevas categorías
        if (!categoriaIds.isEmpty()) {
            for (Long categoriaId : categoriaIds) {
                CategoriaProducto categoria = categoriaRepository.findById(categoriaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + categoriaId));
                
                // Validar que pertenezca a la misma empresa
                if (!categoria.getEmpresa().getId().equals(producto.getEmpresa().getId())) {
                    throw new BusinessException("La categoría " + categoriaId + " no pertenece a la misma empresa");
                }
                
                if (!categoria.getActivo()) {
                    throw new BusinessException("La categoría " + categoria.getNombre() + " está inactiva");
                }
                
                producto.getCategorias().add(categoria);
            }
        }
        
        productoRepository.save(producto);
        log.info("Categorías asignadas al producto {}", productoId);
    }
    
    @Override
    @Transactional
    public void agregarCategoria(Long productoId, Long categoriaId) {
        log.debug("Agregando categoría {} al producto {}", categoriaId, productoId);
        
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
        
        CategoriaProducto categoria = categoriaRepository.findById(categoriaId)
            .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + categoriaId));
        
        // Validaciones
        if (!categoria.getEmpresa().getId().equals(producto.getEmpresa().getId())) {
            throw new BusinessException("La categoría no pertenece a la misma empresa");
        }
        
        if (!categoria.getActivo()) {
            throw new BusinessException("La categoría está inactiva");
        }
        
        if (producto.getCategorias().contains(categoria)) {
            throw new BusinessException("El producto ya está en esta categoría");
        }
        
        producto.getCategorias().add(categoria);
        productoRepository.save(producto);
        
        log.info("Categoría {} agregada al producto {}", categoriaId, productoId);
    }
    
    @Override
    @Transactional
    public void quitarCategoria(Long productoId, Long categoriaId) {
        log.debug("Quitando categoría {} del producto {}", categoriaId, productoId);
        
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
        
        boolean removed = producto.getCategorias().removeIf(cat -> cat.getId().equals(categoriaId));
        
        if (!removed) {
            throw new BusinessException("El producto no pertenece a esa categoría");
        }
        
        productoRepository.save(producto);
        log.info("Categoría {} quitada del producto {}", categoriaId, productoId);
    }
    
    @Override
    @Transactional
    public void quitarTodasLasCategorias(Long productoId) {
        log.debug("Quitando todas las categorías del producto {}", productoId);
        
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
        
        producto.getCategorias().clear();
        productoRepository.save(producto);
        
        log.info("Todas las categorías quitadas del producto {}", productoId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Set<Long> obtenerCategoriaIds(Long productoId) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
        
        return producto.getCategorias().stream()
            .map(CategoriaProducto::getId)
            .collect(Collectors.toSet());
    }
}