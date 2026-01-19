package com.snnsoluciones.backnathbitpos.service.producto.handler;

import com.snnsoluciones.backnathbitpos.entity.CategoriaProducto;
import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.CategoriaProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Handler para gestionar categorías de productos
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductoCategoriaHandler {

    private final CategoriaProductoRepository categoriaRepository;

    /**
     * Asigna categorías a un producto
     */
    public void asignarCategorias(Producto producto, Set<Long> categoriaIds) {
        log.debug("Asignando {} categorías al producto ID: {}", categoriaIds.size(), producto.getId());

        if (categoriaIds == null || categoriaIds.isEmpty()) {
            producto.setCategorias(new HashSet<>());
            return;
        }

        Set<CategoriaProducto> categorias = new HashSet<>();
        for (Long categoriaId : categoriaIds) {
            CategoriaProducto categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + categoriaId));
            categorias.add(categoria);
        }

        producto.setCategorias(categorias);
        log.debug("Categorías asignadas exitosamente");
    }

    /**
     * Actualiza las categorías de un producto (reemplaza las existentes)
     */
    public void actualizarCategorias(Producto producto, Set<Long> categoriaIds) {
        log.debug("Actualizando categorías del producto ID: {}", producto.getId());
        
        // Limpiar categorías actuales
        if (producto.getCategorias() != null) {
            producto.getCategorias().clear();
        } else {
            producto.setCategorias(new HashSet<>());
        }
        
        // Asignar nuevas categorías
        if (categoriaIds != null && !categoriaIds.isEmpty()) {
            asignarCategorias(producto, categoriaIds);
        }
    }
}