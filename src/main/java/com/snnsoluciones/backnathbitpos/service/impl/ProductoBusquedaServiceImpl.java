package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.producto.*;
import com.snnsoluciones.backnathbitpos.entity.CategoriaProducto;
import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.ProductoRepository;
import com.snnsoluciones.backnathbitpos.repository.ProductoQueryRepository;
import com.snnsoluciones.backnathbitpos.service.ProductoBusquedaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
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
public class ProductoBusquedaServiceImpl implements ProductoBusquedaService {
    
    private final ProductoRepository productoRepository;
    private final ProductoQueryRepository productoQueryRepository;
    private final ModelMapper modelMapper;
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProductoListDto> listarPorEmpresa(Long empresaId, Pageable pageable) {
        log.debug("Listando productos de empresa: {}", empresaId);
        
        Page<Producto> productos = productoRepository.findByEmpresaIdAndActivoTrue(empresaId, pageable);
        
        return productos.map(this::convertirAListDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProductoListDto> buscar(Long empresaId, String busqueda, Pageable pageable) {
        log.debug("Buscando productos en empresa: {} con término: {}", empresaId, busqueda);
        
        if (busqueda == null || busqueda.trim().isEmpty()) {
            return listarPorEmpresa(empresaId, pageable);
        }
        
        Page<Producto> productos = productoRepository.buscarPorEmpresa(empresaId, busqueda.trim(), pageable);
        
        return productos.map(this::convertirAListDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProductoListDto> listarPorCategoria(Long categoriaId, Pageable pageable) {
        log.debug("Listando productos de categoría: {}", categoriaId);
        
        Page<Producto> productos = productoRepository.findByCategoriaId(categoriaId, pageable);
        
        return productos.map(this::convertirAListDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProductoDto buscarPorCodigoInterno(Long empresaId, String codigoInterno) {
        log.debug("Buscando producto por código interno: {} en empresa: {}", codigoInterno, empresaId);
        
        Producto producto = productoRepository.findByCodigoInternoAndEmpresaId(codigoInterno, empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con código: " + codigoInterno));
        
        return convertirADto(producto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProductoDto buscarPorCodigoBarras(Long empresaId, String codigoBarras) {
        log.debug("Buscando producto por código de barras: {} en empresa: {}", codigoBarras, empresaId);
        
        Producto producto = productoRepository.findByCodigoBarrasAndEmpresaId(codigoBarras, empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con código de barras: " + codigoBarras));
        
        return convertirADto(producto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductoDto> listarProductosSinCategoria(Long empresaId) {
        log.debug("Listando productos sin categoría de empresa: {}", empresaId);
        
        List<Producto> productos = productoRepository.findProductosSinCategoria(empresaId);
        
        return productos.stream()
            .map(this::convertirADto)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProductoEstadisticasDto obtenerEstadisticas(Long empresaId) {
        log.debug("Obteniendo estadísticas de productos para empresa: {}", empresaId);
        
        Map<String, Object> stats = productoQueryRepository.obtenerEstadisticasProductos(empresaId);
        
        ProductoEstadisticasDto estadisticas = new ProductoEstadisticasDto();
        estadisticas.setTotalProductos((Long) stats.get("totalProductos"));
        estadisticas.setTotalServicios((Long) stats.get("totalServicios"));
        estadisticas.setTotalBienes(estadisticas.getTotalProductos() - estadisticas.getTotalServicios());
        estadisticas.setProductosSinCategoria((Long) stats.get("productosSinCategoria"));
        
        // Convertir productos por categoría
        @SuppressWarnings("unchecked")
        List<Object[]> porCategoria = (List<Object[]>) stats.get("productosPorCategoria");
        Map<String, Long> productosPorCategoria = new HashMap<>();
        for (Object[] row : porCategoria) {
            productosPorCategoria.put((String) row[0], (Long) row[1]);
        }
        estadisticas.setProductosPorCategoria(productosPorCategoria);
        
        return estadisticas;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Long contarProductosActivos(Long empresaId) {
        return productoRepository.countByEmpresaIdAndActivoTrue(empresaId);
    }
    
    // Método auxiliar para convertir a DTO de lista (más ligero)
    private ProductoListDto convertirAListDto(Producto producto) {
        ProductoListDto dto = modelMapper.map(producto, ProductoListDto.class);
        dto.setEmpresaId(producto.getEmpresa().getId());
        
        // Categorías (solo nombres)
        dto.setCategoriasNombres(
            producto.getCategorias().stream()
                .map(CategoriaProducto::getNombre)
                .collect(Collectors.toList())
        );
        
        return dto;
    }
    
    private ProductoDto convertirADto(Producto producto) {
        ProductoDto dto = modelMapper.map(producto, ProductoDto.class);
        dto.setEmpresaId(producto.getEmpresa().getId());
        
        // Mapear categorías
        dto.setCategoriaProductoDtos(
            producto.getCategorias().stream()
                .map(cat -> modelMapper.map(cat, CategoriaProductoDto.class))
                .collect(Collectors.toSet())
        );
        
        return dto;
    }
}