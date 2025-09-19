// ProductoServiceV2.java (ACTUALIZADO)
package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.producto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductoServiceV2 {

    // ========== CRUD BÁSICO ==========
    ProductoDto crear(Long empresaId, ProductoCreateDto dto, MultipartFile imagen);

    ProductoDto crearConImagen(Long empresaId, ProductoCreateDto dto, MultipartFile imagen);

    ProductoDto actualizar(Long empresaId, Long productoId, ProductoUpdateDto dto, MultipartFile imagen);

    void eliminarImagen(Long empresaId, Long productoId);

    void desactivar(Long empresaId, Long productoId);

    void activar(Long empresaId, Long productoId);

    void eliminar(Long empresaId, Long productoId);

    // ========== BÚSQUEDAS SIMPLES ==========
    ProductoDto buscarPorId(Long empresaId, Long productoId);

    Page<ProductoDto> buscarPorEmpresa(Long empresaId, Pageable pageable);

    Page<ProductoDto> buscarPorSucursal(Long sucursalId, Pageable pageable);

    ProductoDto buscarPorCodigo(Long empresaId, String codigoInterno);

    ProductoDto buscarPorCodigoBarras(String codigoBarras, Long empresaId);

    Page<ProductoDto> buscarActivos(Long empresaId, Pageable pageable);

    // ========== BÚSQUEDA CON PAGINACIÓN ==========
    Page<ProductoDto> buscarPaginado(Long empresaId, String termino, Pageable pageable);

    // ========== UTILIDADES ==========
    String generarCodigoInterno(Long empresaId);

    boolean existeCodigoInterno(Long empresaId, String codigo);

    boolean existeCodigoBarras(String codigoBarras, Long empresaId);
}