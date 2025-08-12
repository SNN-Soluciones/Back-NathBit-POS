package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.service.ProductoCategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@Tag(name = "Productos - Categorías", description = "Gestión de categorías de productos")
public class ProductoCategoriaController {
    
    private final ProductoCategoriaService categoriaService;
    
    @PutMapping("/{productoId}/categorias")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Asignar categorías", description = "Asigna un conjunto de categorías a un producto (reemplaza las existentes)")
    public ResponseEntity<ApiResponse<Void>> asignarCategorias(
            @PathVariable Long productoId,
            @RequestBody Set<Long> categoriaIds) {
        
        log.info("Asignando {} categorías al producto {}", categoriaIds.size(), productoId);
        categoriaService.asignarCategorias(productoId, categoriaIds);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
            .success(true)
            .message("Categorías asignadas exitosamente")
            .build());
    }
    
    @PostMapping("/{productoId}/categorias/{categoriaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Agregar categoría", description = "Agrega una categoría adicional al producto")
    public ResponseEntity<ApiResponse<Void>> agregarCategoria(
            @PathVariable Long productoId,
            @PathVariable Long categoriaId) {
        
        log.info("Agregando categoría {} al producto {}", categoriaId, productoId);
        categoriaService.agregarCategoria(productoId, categoriaId);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
            .success(true)
            .message("Categoría agregada exitosamente")
            .build());
    }
    
    @DeleteMapping("/{productoId}/categorias/{categoriaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Quitar categoría", description = "Quita una categoría del producto")
    public ResponseEntity<ApiResponse<Void>> quitarCategoria(
            @PathVariable Long productoId,
            @PathVariable Long categoriaId) {
        
        log.info("Quitando categoría {} del producto {}", categoriaId, productoId);
        categoriaService.quitarCategoria(productoId, categoriaId);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
            .success(true)
            .message("Categoría quitada exitosamente")
            .build());
    }
    
    @DeleteMapping("/{productoId}/categorias")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Quitar todas las categorías", description = "Quita todas las categorías del producto")
    public ResponseEntity<ApiResponse<Void>> quitarTodasLasCategorias(@PathVariable Long productoId) {
        
        log.info("Quitando todas las categorías del producto {}", productoId);
        categoriaService.quitarTodasLasCategorias(productoId);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
            .success(true)
            .message("Todas las categorías quitadas exitosamente")
            .build());
    }
    
    @GetMapping("/{productoId}/categorias/ids")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO')")
    @Operation(summary = "Obtener IDs de categorías", description = "Obtiene los IDs de las categorías del producto")
    public ResponseEntity<ApiResponse<Set<Long>>> obtenerCategoriaIds(@PathVariable Long productoId) {
        
        Set<Long> categoriaIds = categoriaService.obtenerCategoriaIds(productoId);
        
        return ResponseEntity.ok(ApiResponse.<Set<Long>>builder()
            .success(true)
            .message("IDs de categorías obtenidos")
            .data(categoriaIds)
            .build());
    }
}