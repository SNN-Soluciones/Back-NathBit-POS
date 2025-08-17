package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.CategoriaProducto;
import java.util.List;

public interface CategoriaProductoService {
    
    CategoriaProducto buscarPorId(Long id);
    
    List<CategoriaProducto> listarPorEmpresa(Long empresaId, String busqueda);
    
    boolean existePorNombreYEmpresa(String nombre, Long empresaId);
    
    CategoriaProducto crear(CategoriaProducto categoria);
    
    CategoriaProducto actualizar(Long id, CategoriaProducto categoria);
    
    boolean tieneProductosActivos(Long categoriaId);
    
    long contarProductosActivos(Long categoriaId);
    
    Integer obtenerSiguienteOrden(Long empresaId);
}