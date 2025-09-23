package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.producto.*;
import com.snnsoluciones.backnathbitpos.service.ProductoCompuestoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/productos/{productoId}/compuesto")
@RequiredArgsConstructor
@Tag(name = "Productos Compuestos", description = "Gestión de productos personalizables")
public class ProductoCompuestoController {

    private final ProductoCompuestoService compuestoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Configurar producto compuesto", 
               description = "Crea la configuración de slots y opciones para un producto compuesto")
    public ResponseEntity<ApiResponse<ProductoCompuestoDto>> configurar(
            @RequestParam Long empresaId,
            @PathVariable Long productoId,
            @Valid @RequestBody ProductoCompuestoRequest request) {
        
        log.info("Configurando producto compuesto: {} para empresa: {}", productoId, empresaId);
        
        try {
            ProductoCompuestoDto resultado = compuestoService.crear(empresaId, productoId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Producto compuesto configurado exitosamente", resultado));
        } catch (Exception e) {
            log.error("Error configurando producto compuesto", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO')")
    @Operation(summary = "Obtener configuración", 
               description = "Obtiene la configuración completa del producto compuesto")
    public ResponseEntity<ApiResponse<ProductoCompuestoDto>> obtener(
            @RequestParam Long empresaId,
            @PathVariable Long productoId) {
        
        try {
            ProductoCompuestoDto compuesto = compuestoService.buscarPorProductoId(empresaId, productoId);
            return ResponseEntity.ok(ApiResponse.ok("Configuración obtenida", compuesto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("No encontrado: " + e.getMessage()));
        }
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Actualizar configuración", 
               description = "Actualiza slots y opciones del producto compuesto")
    public ResponseEntity<ApiResponse<ProductoCompuestoDto>> actualizar(
            @RequestParam Long empresaId,
            @PathVariable Long productoId,
            @Valid @RequestBody ProductoCompuestoRequest request) {
        
        log.info("Actualizando producto compuesto: {}", productoId);
        
        try {
            // Primero eliminar configuración existente
            compuestoService.eliminar(empresaId, productoId);
            // Recrear con nueva configuración
            ProductoCompuestoDto resultado = compuestoService.crear(empresaId, productoId, request);
            return ResponseEntity.ok(ApiResponse.ok("Configuración actualizada", resultado));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN')")
    @Operation(summary = "Eliminar configuración", 
               description = "Elimina toda la configuración del producto compuesto")
    public ResponseEntity<ApiResponse<Void>> eliminar(
            @RequestParam Long empresaId,
            @PathVariable Long productoId) {
        
        log.info("Eliminando configuración de producto compuesto: {}", productoId);
        
        try {
            compuestoService.eliminar(empresaId, productoId);
            return ResponseEntity.ok(ApiResponse.ok("Configuración eliminada", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }
}