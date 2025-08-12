package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoImpuestoDto;
import com.snnsoluciones.backnathbitpos.entity.ProductoImpuesto;
import com.snnsoluciones.backnathbitpos.service.ProductoImpuestoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@Tag(name = "Productos - Impuestos", description = "Gestión de impuestos de productos")
public class ProductoImpuestoController {
    
    private final ProductoImpuestoService impuestoService;
    private final ModelMapper modelMapper;
    
    @GetMapping("/{productoId}/impuestos")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO')")
    @Operation(summary = "Listar impuestos", description = "Obtiene todos los impuestos de un producto")
    public ResponseEntity<ApiResponse<List<ProductoImpuestoDto>>> obtenerImpuestos(@PathVariable Long productoId) {
        
        List<ProductoImpuesto> impuestos = impuestoService.obtenerImpuestos(productoId);
        List<ProductoImpuestoDto> impuestosDto = impuestos.stream()
            .map(imp -> modelMapper.map(imp, ProductoImpuestoDto.class))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.<List<ProductoImpuestoDto>>builder()
            .success(true)
            .message("Impuestos obtenidos")
            .data(impuestosDto)
            .build());
    }
    
    @PostMapping("/{productoId}/impuestos")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Agregar impuesto", description = "Agrega un nuevo impuesto al producto")
    public ResponseEntity<ApiResponse<ProductoImpuestoDto>> agregarImpuesto(
            @PathVariable Long productoId,
            @Valid @RequestBody ProductoImpuestoDto dto) {
        
        log.info("Agregando impuesto {} al producto {}", dto.getTipoImpuesto(), productoId);
        ProductoImpuesto impuesto = impuestoService.agregarImpuesto(productoId, dto);
        ProductoImpuestoDto impuestoDto = modelMapper.map(impuesto, ProductoImpuestoDto.class);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.<ProductoImpuestoDto>builder()
                .success(true)
                .message("Impuesto agregado exitosamente")
                .data(impuestoDto)
                .build());
    }
    
    @PutMapping("/{productoId}/impuestos")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Actualizar impuestos", description = "Actualiza todos los impuestos del producto (reemplaza existentes)")
    public ResponseEntity<ApiResponse<Void>> actualizarImpuestos(
            @PathVariable Long productoId,
            @Valid @RequestBody List<ProductoImpuestoDto> impuestos) {
        
        log.info("Actualizando {} impuestos del producto {}", impuestos.size(), productoId);
        impuestoService.actualizarImpuestos(productoId, impuestos);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
            .success(true)
            .message("Impuestos actualizados exitosamente")
            .build());
    }
    
    @DeleteMapping("/{productoId}/impuestos/{impuestoId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Quitar impuesto", description = "Quita un impuesto específico del producto")
    public ResponseEntity<ApiResponse<Void>> quitarImpuesto(
            @PathVariable Long productoId,
            @PathVariable Long impuestoId) {
        
        log.info("Quitando impuesto {} del producto {}", impuestoId, productoId);
        impuestoService.quitarImpuesto(productoId, impuestoId);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
            .success(true)
            .message("Impuesto eliminado exitosamente")
            .build());
    }
    
    @DeleteMapping("/{productoId}/impuestos")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Quitar todos los impuestos", description = "Elimina todos los impuestos del producto")
    public ResponseEntity<ApiResponse<Void>> quitarTodosLosImpuestos(@PathVariable Long productoId) {
        
        log.info("Quitando todos los impuestos del producto {}", productoId);
        impuestoService.quitarTodosLosImpuestos(productoId);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
            .success(true)
            .message("Todos los impuestos eliminados exitosamente")
            .build());
    }
}