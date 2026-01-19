package com.snnsoluciones.backnathbitpos.service.producto;

import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCreateDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoListDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoUpdateDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/**
 * Servicio principal para gestión de productos V3.
 * Soporta productos globales (empresaId) y locales (empresaId + sucursalId).
 */
public interface ProductoService {

    // ==================== CRUD BÁSICO ====================

    /**
     * Crea un nuevo producto con imagen opcional.
     * 
     * Si sucursalId es NULL → producto GLOBAL (disponible en toda la empresa)
     * Si sucursalId tiene valor → producto LOCAL (solo esa sucursal)
     * 
     * @param dto Datos del producto a crear
     * @param imagen Imagen opcional del producto
     * @return Producto creado con todos sus datos
     */
    ProductoDto crear(ProductoCreateDto dto, MultipartFile imagen);

    /**
     * Actualiza un producto existente con imagen opcional.
     * 
     * REGLAS:
     * - NO se puede cambiar empresaId
     * - NO se puede cambiar sucursalId
     * - NO se puede cambiar codigoInterno
     * - Si viene imagen nueva, reemplaza la anterior
     * 
     * @param productoId ID del producto a actualizar
     * @param dto Datos a actualizar (solo los que vienen se modifican)
     * @param imagen Nueva imagen opcional (si viene, reemplaza la anterior)
     * @return Producto actualizado
     */
    ProductoDto actualizar(Long productoId, ProductoUpdateDto dto, MultipartFile imagen);

    /**
     * Elimina un producto (borrado lógico: activo = false)
     * 
     * @param productoId ID del producto a eliminar
     */
    void eliminar(Long productoId);

    /**
     * Activa un producto
     * 
     * @param productoId ID del producto
     */
    void activar(Long productoId);

    /**
     * Desactiva un producto
     * 
     * @param productoId ID del producto
     */
    void desactivar(Long productoId);

    // ==================== CONSULTAS ====================

    /**
     * Obtiene un producto por ID con todos sus datos
     * 
     * @param productoId ID del producto
     * @return Producto completo
     */
    ProductoDto obtenerPorId(Long productoId);

    /**
     * Obtiene un producto por código interno
     * 
     * @param codigoInterno Código interno del producto
     * @param empresaId ID de la empresa
     * @return Producto encontrado
     */
    ProductoDto obtenerPorCodigo(String codigoInterno, Long empresaId);

    /**
     * Lista productos con paginación.
     * 
     * ESTRATEGIA DE CONSULTA:
     * - Si solo viene empresaId → devuelve SOLO productos GLOBALES (sucursalId = NULL)
     * - Si viene empresaId + sucursalId → devuelve GLOBALES + LOCALES de esa sucursal
     * 
     * @param empresaId ID de la empresa (obligatorio)
     * @param sucursalId ID de la sucursal (opcional)
     * @param pageable Configuración de paginación
     * @return Página de productos (optimizada con ProductoListDto)
     */
    Page<ProductoListDto> listar(Long empresaId, Long sucursalId, Pageable pageable);

    /**
     * Lista solo productos activos
     * 
     * @param empresaId ID de la empresa
     * @param sucursalId ID de la sucursal (opcional)
     * @param pageable Configuración de paginación
     * @return Página de productos activos
     */
    Page<ProductoListDto> listarActivos(Long empresaId, Long sucursalId, Pageable pageable);

    // ==================== BÚSQUEDAS ====================

    /**
     * Busca productos por término (código interno, código barras, nombre).
     * 
     * ESTRATEGIA:
     * - Si solo empresaId → busca en productos GLOBALES
     * - Si empresaId + sucursalId → busca en GLOBALES + LOCALES
     * 
     * @param empresaId ID de la empresa
     * @param sucursalId ID de la sucursal (opcional)
     * @param termino Texto a buscar
     * @param pageable Configuración de paginación
     * @return Página de productos que coinciden
     */
    Page<ProductoListDto> buscar(Long empresaId, Long sucursalId, String termino, Pageable pageable);

    /**
     * Busca productos por categoría
     * 
     * @param categoriaId ID de la categoría
     * @param empresaId ID de la empresa
     * @param sucursalId ID de la sucursal (opcional)
     * @param pageable Configuración de paginación
     * @return Productos de esa categoría
     */
    Page<ProductoListDto> buscarPorCategoria(Long categoriaId, Long empresaId, Long sucursalId, Pageable pageable);

    /**
     * Busca productos por familia
     * 
     * @param familiaId ID de la familia
     * @param empresaId ID de la empresa
     * @param sucursalId ID de la sucursal (opcional)
     * @param pageable Configuración de paginación
     * @return Productos de esa familia
     */
    Page<ProductoListDto> buscarPorFamilia(Long familiaId, Long empresaId, Long sucursalId, Pageable pageable);

    // ==================== IMÁGENES ====================

    /**
     * Actualiza solo la imagen de un producto
     * 
     * @param productoId ID del producto
     * @param imagen Nueva imagen
     */
    void actualizarImagen(Long productoId, MultipartFile imagen);

    /**
     * Elimina la imagen de un producto
     * 
     * @param productoId ID del producto
     */
    void eliminarImagen(Long productoId);

    // ==================== UTILIDADES ====================

    /**
     * Genera un código interno único para la empresa.
     * Formato: PROD-XXXXX (autoincremental por empresa)
     * 
     * @param empresaId ID de la empresa
     * @return Código interno generado
     */
    String generarCodigoInterno(Long empresaId);

    /**
     * Valida si un código interno ya existe en la empresa
     * 
     * @param codigoInterno Código a validar
     * @param empresaId ID de la empresa
     * @return true si existe, false si está disponible
     */
    boolean existeCodigoInterno(String codigoInterno, Long empresaId);

    /**
     * Valida si un código de barras ya existe
     * 
     * @param codigoBarras Código de barras a validar
     * @return true si existe, false si está disponible
     */
    boolean existeCodigoBarras(String codigoBarras);
}