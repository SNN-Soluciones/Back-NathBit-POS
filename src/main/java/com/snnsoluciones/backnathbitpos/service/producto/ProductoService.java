package com.snnsoluciones.backnathbitpos.service.producto;

import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCreateDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoUpdateDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Servicio principal para gestión de productos.
 * Orquesta todos los handlers y proporciona la API pública para productos.
 */
public interface ProductoService {

    // ==================== CRUD BÁSICO ====================

    /**
     * Crea un nuevo producto con imagen opcional
     */
    ProductoDto crear(ProductoCreateDto dto, MultipartFile imagen);

    /**
     * Actualiza un producto existente con imagen opcional
     */
    ProductoDto actualizar(Long productoId, ProductoUpdateDto dto, MultipartFile imagen);

    /**
     * Elimina un producto (borrado físico)
     */
    void eliminar(Long productoId);

    /**
     * Activa un producto
     */
    void activar(Long productoId);

    /**
     * Desactiva un producto
     */
    void desactivar(Long productoId);

    // ==================== CONSULTAS ====================

    /**
     * Obtiene un producto por ID
     */
    ProductoDto obtenerPorId(Long productoId);

    /**
     * Obtiene un producto por código interno
     */
    ProductoDto obtenerPorCodigo(String codigoInterno, Long empresaId);

    /**
     * Lista productos con paginación
     */
    Page<ProductoDto> listar(Long empresaId, Pageable pageable);

    /**
     * Lista solo productos activos
     */
    Page<ProductoDto> listarActivos(Long empresaId, Pageable pageable);

    // ==================== BÚSQUEDAS ====================

    /**
     * Busca productos por término (código, nombre, descripción)
     */
    Page<ProductoDto> buscar(Long empresaId, String termino, Pageable pageable);

    /**
     * Busca productos de una sucursal con paginación
     */
    Page<ProductoDto> buscarPorSucursal(Long sucursalId, Pageable pageable);

    /**
     * Busca productos en una sucursal por término
     */
    Page<ProductoDto> buscarPorSucursal(Long sucursalId, String termino, Pageable pageable);


    /**
     * Busca productos por categoría
     */
    Page<ProductoDto> buscarPorCategoria(Long categoriaId, Pageable pageable);

    /**
     * Busca productos por familia
     */
    Page<ProductoDto> buscarPorFamilia(Long familiaId, Pageable pageable);

    // ==================== UTILIDADES ====================

    /**
     * Genera un código interno único para la empresa
     */
    String generarCodigoInterno(Long empresaId);

    /**
     * Verifica si existe un código interno
     */
    boolean existeCodigo(String codigoInterno, Long empresaId);

    /**
     * Actualiza el precio de venta de un producto
     */
    void actualizarPrecio(Long productoId, BigDecimal nuevoPrecio);

    // ==================== IMÁGENES ====================

    /**
     * Actualiza solo la imagen de un producto
     */
    void actualizarImagen(Long productoId, MultipartFile imagen);

    /**
     * Elimina la imagen de un producto
     */
    void eliminarImagen(Long productoId);

    // ==================== CATEGORÍAS ====================

    /**
     * Asigna categorías a un producto (reemplaza las existentes)
     */
    void asignarCategorias(Long productoId, Set<Long> categoriaIds);

    /**
     * Agrega una categoría sin eliminar las existentes
     */
    void agregarCategoria(Long productoId, Long categoriaId);

    /**
     * Quita una categoría específica
     */
    void quitarCategoria(Long productoId, Long categoriaId);
}