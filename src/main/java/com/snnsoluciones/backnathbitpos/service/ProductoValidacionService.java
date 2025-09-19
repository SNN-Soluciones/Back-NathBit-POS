package com.snnsoluciones.backnathbitpos.service;

@Deprecated(since = "2.0", forRemoval = true)
public interface ProductoValidacionService {
    
    // Validaciones de unicidad
    boolean existeCodigoInterno(String codigo, Long empresaId, Long excludeId);
    boolean existeCodigoBarras(String codigo, Long empresaId, Long excludeId);
    boolean existeNombre(String nombre, Long empresaId, Long excludeId);
    
    // Validaciones de negocio
    void validarProductoParaVenta(Long productoId);
    void validarCambioCategoria(Long productoId, Long categoriaId);
}