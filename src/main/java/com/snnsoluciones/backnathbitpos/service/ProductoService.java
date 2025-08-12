package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.producto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductoService {
    
    // CRUD básico
    ProductoDto crear(Long empresaId, ProductoCreateDto dto);
    ProductoDto actualizar(Long empresaId, Long productoId, ProductoUpdateDto dto);
    ProductoDto obtenerPorId(Long empresaId, Long productoId);
    void eliminar(Long empresaId, Long productoId);
    void activarDesactivar(Long empresaId, Long productoId, boolean activo);
    
    // Búsquedas
    Page<ProductoListDto> listar(Long empresaId, Pageable pageable);
    Page<ProductoListDto> buscar(Long empresaId, String busqueda, Pageable pageable);
    Page<ProductoListDto> listarPorCategoria(Long empresaId, Long categoriaId, Pageable pageable);
    
    // Búsquedas específicas
    ProductoDto buscarPorCodigo(Long empresaId, String codigoInterno);
    ProductoDto buscarPorCodigoBarras(Long empresaId, String codigoBarras);
    
    // Operaciones masivas
    ProductoImportResultDto importarDesdeExcel(Long empresaId, MultipartFile archivo);
    void actualizarPreciosMasivo(Long empresaId, List<Long> productosIds, Double porcentaje);
    
    // Gestión de impuestos
    ProductoDto agregarImpuesto(Long empresaId, Long productoId, ProductoImpuestoDto impuesto);
    ProductoDto quitarImpuesto(Long empresaId, Long productoId, Long impuestoId);
    void actualizarImpuestos(Long empresaId, Long productoId, List<ProductoImpuestoDto> impuestos);
    
    // Validaciones
    boolean existeCodigoInterno(Long empresaId, String codigoInterno);
    boolean existeCodigoBarras(Long empresaId, String codigoBarras);
    boolean existeNombre(Long empresaId, String nombre);
    
    // Reportes
    Long contarProductosActivos(Long empresaId);
    List<ProductoDto> productosConBajoInventario(Long empresaId);
}