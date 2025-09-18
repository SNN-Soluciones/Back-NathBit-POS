package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.producto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductoBusquedaService {
    
    // Búsquedas paginadas
    List<ProductoDto> buscarPorTerminoConContexto(Long empresaId, String termino);
    Page<ProductoListDto> listarPorEmpresa(Long empresaId, Pageable pageable);
    Page<ProductoListDto> buscar(Long empresaId, String busqueda, Pageable pageable);
    Page<ProductoListDto> listarPorCategoria(Long categoriaId, Pageable pageable);
    
    // Búsquedas específicas
    ProductoDto buscarPorCodigoInterno(Long empresaId, String codigoInterno);
    ProductoDto buscarPorCodigoBarras(Long empresaId, String codigoBarras);
    List<ProductoDto> listarProductosSinCategoria(Long empresaId);
    
    // Estadísticas
    ProductoEstadisticasDto obtenerEstadisticas(Long empresaId);
}