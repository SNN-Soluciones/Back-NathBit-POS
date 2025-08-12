package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.service.ProductoValidacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@Tag(name = "Productos - Validaciones", description = "Validaciones de productos")
public class ProductoValidacionController {
    
    private final ProductoValidacionService validacionService;
    
    @GetMapping("/{empresaId}/validar/codigo-interno")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Validar código interno", description = "Verifica si un código interno ya existe")
    public ResponseEntity<ApiResponse<Boolean>> validarCodigoInterno(
            @PathVariable Long empresaId,
            @RequestParam String codigo,
            @RequestParam(required = false) Long excludeId) {
        
        boolean existe = validacionService.existeCodigoInterno(codigo, empresaId, excludeId);
        
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
            .success(true)
            .message(existe ? "El código ya existe" : "El código está disponible")
            .data(existe)
            .build());
    }
    
    @GetMapping("/{empresaId}/validar/codigo-barras")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Validar código de barras", description = "Verifica si un código de barras ya existe")
    public ResponseEntity<ApiResponse<Boolean>> validarCodigoBarras(
            @PathVariable Long empresaId,
            @RequestParam String codigo,
            @RequestParam(required = false) Long excludeId) {
        
        boolean existe = validacionService.existeCodigoBarras(codigo, empresaId, excludeId);
        
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
            .success(true)
            .message(existe ? "El código de barras ya existe" : "El código de barras está disponible")
            .data(existe)
            .build());
    }
    
    @GetMapping("/{empresaId}/validar/nombre")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Validar nombre", description = "Verifica si un nombre de producto ya existe")
    public ResponseEntity<ApiResponse<Boolean>> validarNombre(
            @PathVariable Long empresaId,
            @RequestParam String nombre,
            @RequestParam(required = false) Long excludeId) {
        
        boolean existe = validacionService.existeNombre(nombre, empresaId, excludeId);
        
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
            .success(true)
            .message(existe ? "El nombre ya existe" : "El nombre está disponible")
            .data(existe)
            .build());
    }
    
    @PostMapping("/{productoId}/validar/venta")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO')")
    @Operation(summary = "Validar para venta", description = "Valida si un producto puede venderse")
    public ResponseEntity<ApiResponse<Void>> validarParaVenta(@PathVariable Long productoId) {
        
        try {
            validacionService.validarProductoParaVenta(productoId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Producto válido para venta")
                .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
    
    @PostMapping("/{productoId}/validar/cambio-categoria/{categoriaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Validar cambio de categoría", description = "Valida si un producto puede cambiarse a una categoría")
    public ResponseEntity<ApiResponse<Void>> validarCambioCategoria(
            @PathVariable Long productoId,
            @PathVariable Long categoriaId) {
        
        try {
            validacionService.validarCambioCategoria(productoId, categoriaId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Cambio de categoría válido")
                .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
    
    @GetMapping("/{empresaId}/validar/todos")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Validar todos los campos", description = "Valida código interno, código de barras y nombre de una vez")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> validarTodos(
            @PathVariable Long empresaId,
            @RequestParam(required = false) String codigoInterno,
            @RequestParam(required = false) String codigoBarras,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Long excludeId) {
        
        Map<String, Boolean> validaciones = new HashMap<>();
        
        if (codigoInterno != null) {
            validaciones.put("codigoInternoExiste", 
                validacionService.existeCodigoInterno(codigoInterno, empresaId, excludeId));
        }
        
        if (codigoBarras != null) {
            validaciones.put("codigoBarrasExiste", 
                validacionService.existeCodigoBarras(codigoBarras, empresaId, excludeId));
        }
        
        if (nombre != null) {
            validaciones.put("nombreExiste", 
                validacionService.existeNombre(nombre, empresaId, excludeId));
        }
        
        return ResponseEntity.ok(ApiResponse.<Map<String, Boolean>>builder()
            .success(true)
            .message("Validaciones completadas")
            .data(validaciones)
            .build());
    }
}