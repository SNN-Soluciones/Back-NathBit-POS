package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.producto.*;
import com.snnsoluciones.backnathbitpos.service.ProductoBusquedaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@Tag(name = "Productos - Búsquedas", description = "Búsquedas y consultas de productos")
public class ProductoBusquedaController {
    
    private final ProductoBusquedaService busquedaService;
    
    @GetMapping("/{empresaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO', 'MESERO')")
    @Operation(summary = "Listar productos", description = "Lista todos los productos de una empresa con paginación")
    public ResponseEntity<ApiResponse<Page<ProductoListDto>>> listar(
            @PathVariable Long empresaId,
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {
        
        Page<ProductoListDto> productos = busquedaService.listarPorEmpresa(empresaId, pageable);
        
        return ResponseEntity.ok(ApiResponse.<Page<ProductoListDto>>builder()
            .success(true)
            .message("Productos encontrados")
            .data(productos)
            .build());
    }
    
    @GetMapping("/{empresaId}/buscar")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO', 'MESERO')")
    @Operation(summary = "Buscar productos", description = "Busca productos por código, nombre o descripción")
    public ResponseEntity<ApiResponse<Page<ProductoListDto>>> buscar(
            @PathVariable Long empresaId,
            @RequestParam(required = false) String busqueda,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<ProductoListDto> productos = busquedaService.buscar(empresaId, busqueda, pageable);
        
        return ResponseEntity.ok(ApiResponse.<Page<ProductoListDto>>builder()
            .success(true)
            .message("Resultados de búsqueda")
            .data(productos)
            .build());
    }
    
    @GetMapping("/categoria/{categoriaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO', 'MESERO')")
    @Operation(summary = "Listar por categoría", description = "Lista productos de una categoría específica")
    public ResponseEntity<ApiResponse<Page<ProductoListDto>>> listarPorCategoria(
            @PathVariable Long categoriaId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<ProductoListDto> productos = busquedaService.listarPorCategoria(categoriaId, pageable);
        
        return ResponseEntity.ok(ApiResponse.<Page<ProductoListDto>>builder()
            .success(true)
            .message("Productos de la categoría")
            .data(productos)
            .build());
    }
    
    @GetMapping("/{empresaId}/codigo/{codigoInterno}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO', 'MESERO')")
    @Operation(summary = "Buscar por código interno", description = "Busca un producto por su código interno")
    public ResponseEntity<ApiResponse<ProductoDto>> buscarPorCodigo(
            @PathVariable Long empresaId,
            @PathVariable String codigoInterno) {
        
        ProductoDto producto = busquedaService.buscarPorCodigoInterno(empresaId, codigoInterno);
        
        return ResponseEntity.ok(ApiResponse.<ProductoDto>builder()
            .success(true)
            .message("Producto encontrado")
            .data(producto)
            .build());
    }
    
    @GetMapping("/{empresaId}/codigo-barras/{codigoBarras}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO', 'MESERO')")
    @Operation(summary = "Buscar por código de barras", description = "Busca un producto por su código de barras")
    public ResponseEntity<ApiResponse<ProductoDto>> buscarPorCodigoBarras(
            @PathVariable Long empresaId,
            @PathVariable String codigoBarras) {
        
        ProductoDto producto = busquedaService.buscarPorCodigoBarras(empresaId, codigoBarras);
        
        return ResponseEntity.ok(ApiResponse.<ProductoDto>builder()
            .success(true)
            .message("Producto encontrado")
            .data(producto)
            .build());
    }
    
    @GetMapping("/{empresaId}/sin-categoria")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Productos sin categoría", description = "Lista productos que no tienen categoría asignada")
    public ResponseEntity<ApiResponse<List<ProductoDto>>> listarSinCategoria(@PathVariable Long empresaId) {
        
        List<ProductoDto> productos = busquedaService.listarProductosSinCategoria(empresaId);
        
        return ResponseEntity.ok(ApiResponse.<List<ProductoDto>>builder()
            .success(true)
            .message("Productos sin categoría")
            .data(productos)
            .build());
    }
    
    @GetMapping("/{empresaId}/estadisticas")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Estadísticas de productos", description = "Obtiene estadísticas generales de productos")
    public ResponseEntity<ApiResponse<ProductoEstadisticasDto>> obtenerEstadisticas(@PathVariable Long empresaId) {
        
        ProductoEstadisticasDto estadisticas = busquedaService.obtenerEstadisticas(empresaId);
        
        return ResponseEntity.ok(ApiResponse.<ProductoEstadisticasDto>builder()
            .success(true)
            .message("Estadísticas generadas")
            .data(estadisticas)
            .build());
    }
}