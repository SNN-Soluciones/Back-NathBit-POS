package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.familia.ActualizarFamiliaProductoRequest;
import com.snnsoluciones.backnathbitpos.dto.familia.CrearFamiliaProductoRequest;
import com.snnsoluciones.backnathbitpos.dto.familia.FamiliaProductoDTO;
import com.snnsoluciones.backnathbitpos.service.FamiliaProductoService;
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

/**
 * Controller para gestión de Familias de Productos
 * Solo usuarios ADMIN, SUPER_ADMIN y ROOT tienen acceso
 */
@RestController
@RequestMapping("/api/familias-producto")
@RequiredArgsConstructor
@Tag(name = "Familias de Productos", description = "Gestión de catálogos de familias de productos")
@Slf4j
public class FamiliaProductoController {
    
    private final FamiliaProductoService familiaService;
    
    @Operation(summary = "Listar todas las familias de una empresa")
    @GetMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<FamiliaProductoDTO>>> listar(
            @RequestParam Long empresaId,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false, defaultValue = "false") Boolean soloActivas) {
        
        log.info("GET /api/familias-producto - empresaId: {}, busqueda: {}, soloActivas: {}", 
                empresaId, busqueda, soloActivas);
        
        List<FamiliaProductoDTO> familias;
        
        if (busqueda != null && !busqueda.trim().isEmpty()) {
            familias = familiaService.buscarPorEmpresa(empresaId, busqueda);
        } else if (soloActivas) {
            familias = familiaService.listarActivasPorEmpresa(empresaId);
        } else {
            familias = familiaService.listarPorEmpresa(empresaId);
        }
        
        return ResponseEntity.ok(ApiResponse.<List<FamiliaProductoDTO>>builder()
                .success(true)
                .message("Familias obtenidas exitosamente")
                .data(familias)
                .build());
    }
    
    @Operation(summary = "Obtener una familia por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<FamiliaProductoDTO>> obtenerPorId(
            @PathVariable Long id,
            @RequestParam Long empresaId) {
        
        log.info("GET /api/familias-producto/{} - empresaId: {}", id, empresaId);
        
        FamiliaProductoDTO familia = familiaService.obtenerPorId(id, empresaId);
        
        return ResponseEntity.ok(ApiResponse.<FamiliaProductoDTO>builder()
                .success(true)
                .message("Familia obtenida exitosamente")
                .data(familia)
                .build());
    }
    
    @Operation(summary = "Crear una nueva familia")
    @PostMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<FamiliaProductoDTO>> crear(
            @Valid @RequestBody CrearFamiliaProductoRequest request,
            @RequestParam Long empresaId) {
        
        log.info("POST /api/familias-producto - empresaId: {}, familia: {}", empresaId, request.getNombre());
        
        FamiliaProductoDTO familia = familiaService.crear(request, empresaId);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.<FamiliaProductoDTO>builder()
                        .success(true)
                        .message("Familia creada exitosamente")
                        .data(familia)
                        .build());
    }
    
    @Operation(summary = "Actualizar una familia existente")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<FamiliaProductoDTO>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarFamiliaProductoRequest request,
            @RequestParam Long empresaId) {
        
        log.info("PUT /api/familias-producto/{} - empresaId: {}", id, empresaId);
        
        FamiliaProductoDTO familia = familiaService.actualizar(id, request, empresaId);
        
        return ResponseEntity.ok(ApiResponse.<FamiliaProductoDTO>builder()
                .success(true)
                .message("Familia actualizada exitosamente")
                .data(familia)
                .build());
    }
    
    @Operation(summary = "Eliminar una familia")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(
            @PathVariable Long id,
            @RequestParam Long empresaId) {
        
        log.info("DELETE /api/familias-producto/{} - empresaId: {}", id, empresaId);
        
        familiaService.eliminar(id, empresaId);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Familia eliminada exitosamente")
                .build());
    }
    
    @Operation(summary = "Cambiar estado de una familia (activar/desactivar)")
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<FamiliaProductoDTO>> cambiarEstado(
            @PathVariable Long id,
            @RequestParam Long empresaId,
            @RequestParam Boolean activa) {
        
        log.info("PATCH /api/familias-producto/{}/estado - empresaId: {}, activa: {}", id, empresaId, activa);
        
        FamiliaProductoDTO familia = familiaService.cambiarEstado(id, activa, empresaId);
        
        return ResponseEntity.ok(ApiResponse.<FamiliaProductoDTO>builder()
                .success(true)
                .message("Estado de familia actualizado exitosamente")
                .data(familia)
                .build());
    }
}