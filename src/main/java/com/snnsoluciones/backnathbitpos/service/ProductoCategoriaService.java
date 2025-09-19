package com.snnsoluciones.backnathbitpos.service;

import java.util.Set;


@Deprecated(since = "2.0", forRemoval = true)
public interface ProductoCategoriaService {
    
    // Gestión de categorías
    void asignarCategorias(Long productoId, Set<Long> categoriaIds);
    void agregarCategoria(Long productoId, Long categoriaId);
    void quitarCategoria(Long productoId, Long categoriaId);
    void quitarTodasLasCategorias(Long productoId);
    Set<Long> obtenerCategoriaIds(Long productoId);
}