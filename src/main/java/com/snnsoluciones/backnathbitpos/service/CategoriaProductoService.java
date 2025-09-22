package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.CategoriaProducto;
import java.util.List;
import java.util.Optional;

public interface CategoriaProductoService {
    
    Optional<CategoriaProducto> buscarPorId(Long id);
    
    List<CategoriaProducto> listarPorEmpresa(Long empresaId, Long sucursalId, String busqueda);
    
    boolean existePorNombreYEmpresa(String nombre, Long sucursalId, Long empresaId);
    
    CategoriaProducto crear(CategoriaProducto categoria);
    
    CategoriaProducto actualizar(Long id, CategoriaProducto categoria);
    
    boolean tieneProductosActivos(Long categoriaId);
    
    long contarProductosActivos(Long categoriaId);
    
    Integer obtenerSiguienteOrden(Long empresaId, Long sucursalId);
}