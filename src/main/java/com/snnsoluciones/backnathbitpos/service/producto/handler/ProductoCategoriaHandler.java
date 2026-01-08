package com.snnsoluciones.backnathbitpos.service.producto.handler;

import com.snnsoluciones.backnathbitpos.entity.CategoriaProducto;
import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.CategoriaProductoRepository;
import com.snnsoluciones.backnathbitpos.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handler encargado de gestionar las categorías de productos.
 * Maneja asignación, adición y eliminación de categorías.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductoCategoriaHandler {

    private final CategoriaProductoRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    /**
     * Asigna un conjunto de categorías a un producto (reemplaza las existentes)
     */
    @Transactional
    public void asignarCategorias(Producto producto, Set<Long> categoriaIds) {
        log.debug("Asignando {} categorías al producto ID: {}", categoriaIds.size(), producto.getId());

        if (categoriaIds == null || categoriaIds.isEmpty()) {
            log.debug("No hay categorías para asignar, limpiando categorías existentes");
            producto.getCategorias().clear();
            productoRepository.save(producto);
            return;
        }

        // Validar y obtener categorías
        Set<CategoriaProducto> categorias = validarYObtenerCategorias(
            categoriaIds, 
            producto.getEmpresa().getId()
        );

        // Limpiar categorías actuales y asignar las nuevas
        producto.getCategorias().clear();
        producto.getCategorias().addAll(categorias);
        productoRepository.save(producto);

        log.info("Se asignaron {} categorías al producto ID: {}", categorias.size(), producto.getId());
    }

    /**
     * Agrega una categoría a un producto (sin eliminar las existentes)
     */
    @Transactional
    public void agregarCategoria(Producto producto, Long categoriaId) {
        log.debug("Agregando categoría ID: {} al producto ID: {}", categoriaId, producto.getId());

        // Verificar si ya tiene la categoría
        boolean yaLaTiene = producto.getCategorias().stream()
            .anyMatch(c -> c.getId().equals(categoriaId));

        if (yaLaTiene) {
            log.warn("El producto ya tiene asignada la categoría ID: {}", categoriaId);
            return;
        }

        // Validar y obtener la categoría
        CategoriaProducto categoria = obtenerYValidarCategoria(
            categoriaId, 
            producto.getEmpresa().getId()
        );

        // Agregar categoría
        producto.getCategorias().add(categoria);
        productoRepository.save(producto);

        log.info("Categoría agregada exitosamente al producto ID: {}", producto.getId());
    }

    /**
     * Quita una categoría de un producto
     */
    @Transactional
    public void quitarCategoria(Producto producto, Long categoriaId) {
        log.debug("Quitando categoría ID: {} del producto ID: {}", categoriaId, producto.getId());

        boolean removida = producto.getCategorias().removeIf(c -> c.getId().equals(categoriaId));

        if (!removida) {
            log.warn("El producto no tenía asignada la categoría ID: {}", categoriaId);
            return;
        }

        productoRepository.save(producto);
        log.info("Categoría removida exitosamente del producto ID: {}", producto.getId());
    }

    /**
     * Quita todas las categorías de un producto
     */
    @Transactional
    public void quitarTodasLasCategorias(Producto producto) {
        log.debug("Quitando todas las categorías del producto ID: {}", producto.getId());

        int cantidadAnterior = producto.getCategorias().size();
        producto.getCategorias().clear();
        productoRepository.save(producto);

        log.info("Se quitaron {} categorías del producto ID: {}", cantidadAnterior, producto.getId());
    }

    /**
     * Obtiene los IDs de las categorías de un producto
     */
    public Set<Long> obtenerCategoriaIds(Producto producto) {
        return producto.getCategorias().stream()
            .map(CategoriaProducto::getId)
            .collect(Collectors.toSet());
    }

    /**
     * Valida y obtiene un conjunto de categorías
     */
    private Set<CategoriaProducto> validarYObtenerCategorias(Set<Long> categoriaIds, Long empresaId) {
        Set<CategoriaProducto> categorias = new HashSet<>();

        for (Long categoriaId : categoriaIds) {
            CategoriaProducto categoria = obtenerYValidarCategoria(categoriaId, empresaId);
            categorias.add(categoria);
        }

        return categorias;
    }

    /**
     * Obtiene y valida una categoría individual
     */
    private CategoriaProducto obtenerYValidarCategoria(Long categoriaId, Long empresaId) {
        CategoriaProducto categoria = categoriaRepository.findById(categoriaId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Categoría no encontrada: " + categoriaId
            ));

        // Validar que pertenezca a la misma empresa
        if (!categoria.getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException(
                "La categoría " + categoriaId + " no pertenece a la empresa del producto"
            );
        }

        // Validar que esté activa
        if (!categoria.getActivo()) {
            throw new BusinessException(
                "La categoría '" + categoria.getNombre() + "' está inactiva"
            );
        }

        return categoria;
    }
}