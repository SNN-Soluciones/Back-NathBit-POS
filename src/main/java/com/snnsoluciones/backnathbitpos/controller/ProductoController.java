package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.producto.*;
import com.snnsoluciones.backnathbitpos.service.ProductoCrudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "Gestión de productos")
public class ProductoController {
    
    private final ProductoCrudService productoCrudService;
    
    @PostMapping("/{empresaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Crear producto", description = "Crea un nuevo producto para una empresa")
    public ResponseEntity<ApiResponse<ProductoDto>> crear(
            @PathVariable Long empresaId,
            @Valid @RequestBody ProductoCreateDto dto) {
        
        log.info("Creando producto para empresa: {}", empresaId);
        ProductoDto producto = productoCrudService.crear(empresaId, dto);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.<ProductoDto>builder()
                .success(true)
                .message("Producto creado exitosamente")
                .data(producto)
                .build());
    }
    
    @PutMapping("/{empresaId}/{productoId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Actualizar producto", description = "Actualiza un producto existente")
    public ResponseEntity<ApiResponse<ProductoDto>> actualizar(
            @PathVariable Long empresaId,
            @PathVariable Long productoId,
            @Valid @RequestBody ProductoUpdateDto dto) {
        
        log.info("Actualizando producto: {} de empresa: {}", productoId, empresaId);
        ProductoDto producto = productoCrudService.actualizar(empresaId, productoId, dto);
        
        return ResponseEntity.ok(ApiResponse.<ProductoDto>builder()
            .success(true)
            .message("Producto actualizado exitosamente")
            .data(producto)
            .build());
    }
    
    @GetMapping("/{empresaId}/{productoId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO')")
    @Operation(summary = "Obtener producto", description = "Obtiene un producto por ID")
    public ResponseEntity<ApiResponse<ProductoDto>> obtenerPorId(
            @PathVariable Long empresaId,
            @PathVariable Long productoId) {
        
        ProductoDto producto = productoCrudService.obtenerPorId(empresaId, productoId);
        
        return ResponseEntity.ok(ApiResponse.<ProductoDto>builder()
            .success(true)
            .message("Producto encontrado")
            .data(producto)
            .build());
    }
    
    @DeleteMapping("/{empresaId}/{productoId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Eliminar producto", description = "Elimina un producto")
    public ResponseEntity<ApiResponse<Void>> eliminar(
            @PathVariable Long empresaId,
            @PathVariable Long productoId) {
        
        log.info("Eliminando producto: {} de empresa: {}", productoId, empresaId);
        productoCrudService.eliminar(empresaId, productoId);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
            .success(true)
            .message("Producto eliminado exitosamente")
            .build());
    }
    
    @PatchMapping("/{empresaId}/{productoId}/activar")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Activar/Desactivar producto", description = "Cambia el estado activo del producto")
    public ResponseEntity<ApiResponse<Void>> activarDesactivar(
            @PathVariable Long empresaId,
            @PathVariable Long productoId,
            @RequestParam boolean activo) {
        
        log.info("{} producto: {} de empresa: {}", activo ? "Activando" : "Desactivando", productoId, empresaId);
        productoCrudService.activarDesactivar(empresaId, productoId, activo);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
            .success(true)
            .message("Producto " + (activo ? "activado" : "desactivado") + " exitosamente")
            .build());
    }
    
    @GetMapping("/{empresaId}/generar-codigo")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Generar código interno", description = "Genera un código interno único para producto")
    public ResponseEntity<ApiResponse<String>> generarCodigo(@PathVariable Long empresaId) {
        
        String codigo = productoCrudService.generarCodigoInterno(empresaId);
        
        return ResponseEntity.ok(ApiResponse.<String>builder()
            .success(true)
            .message("Código generado")
            .data(codigo)
            .build());
    }
}