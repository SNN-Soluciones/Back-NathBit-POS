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
 * Soporta familias globales (empresa) y específicas por sucursal
 * Solo usuarios ADMIN, SUPER_ADMIN y ROOT tienen acceso
 */
@RestController
@RequestMapping("/api/familias-producto")
@RequiredArgsConstructor
@Tag(name = "Familias de Productos", description = "Gestión de catálogos de familias de productos")
@Slf4j
public class FamiliaProductoController {

    private final FamiliaProductoService familiaService;

    @Operation(summary = "Listar familias por empresa y opcionalmente por sucursal",
        description = "Si sucursalId=0 busca familias globales. Si sucursalId>0 busca globales + específicas de esa sucursal")
    @GetMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<FamiliaProductoDTO>>> listar(
        @RequestParam Long empresaId,
        @RequestParam(required = false, defaultValue = "0") Long sucursalId,
        @RequestParam(required = false) String busqueda,
        @RequestParam(required = false, defaultValue = "false") Boolean soloActivas) {

        log.info("GET /api/familias-producto - empresaId: {}, sucursalId: {}, busqueda: {}, soloActivas: {}",
            empresaId, sucursalId, busqueda, soloActivas);

        List<FamiliaProductoDTO> familias;

        if (busqueda != null && !busqueda.trim().isEmpty()) {
            familias = familiaService.buscarPorEmpresaYSucursal(empresaId, sucursalId, busqueda);
        } else if (soloActivas) {
            familias = familiaService.listarActivasPorEmpresaYSucursal(empresaId, sucursalId);
        } else {
            familias = familiaService.listarPorEmpresaYSucursal(empresaId, sucursalId);
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
        @RequestParam Long empresaId,
        @RequestParam(required = false, defaultValue = "0") Long sucursalId) {

        log.info("GET /api/familias-producto/{} - empresaId: {}, sucursalId: {}", id, empresaId, sucursalId);

        FamiliaProductoDTO familia = familiaService.obtenerPorId(id, empresaId, sucursalId);

        return ResponseEntity.ok(ApiResponse.<FamiliaProductoDTO>builder()
            .success(true)
            .message("Familia obtenida exitosamente")
            .data(familia)
            .build());
    }

    @Operation(summary = "Crear una nueva familia",
        description = "Si sucursalId=0 crea familia global. Si sucursalId>0 crea familia específica de esa sucursal")
    @PostMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<FamiliaProductoDTO>> crear(
        @Valid @RequestBody CrearFamiliaProductoRequest request,
        @RequestParam Long empresaId,
        @RequestParam(required = false, defaultValue = "0") Long sucursalId) {

        log.info("POST /api/familias-producto - empresaId: {}, sucursalId: {}, familia: {}",
            empresaId, sucursalId, request.getNombre());

        FamiliaProductoDTO familia = familiaService.crear(request, empresaId, sucursalId);

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
        @RequestParam Long empresaId,
        @RequestParam(required = false, defaultValue = "0") Long sucursalId) {

        log.info("PUT /api/familias-producto/{} - empresaId: {}, sucursalId: {}", id, empresaId, sucursalId);

        FamiliaProductoDTO familia = familiaService.actualizar(id, request, empresaId, sucursalId);

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
        @RequestParam Long empresaId,
        @RequestParam(required = false, defaultValue = "0") Long sucursalId) {

        log.info("DELETE /api/familias-producto/{} - empresaId: {}, sucursalId: {}", id, empresaId, sucursalId);

        familiaService.eliminar(id, empresaId, sucursalId);

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
        @RequestParam(required = false, defaultValue = "0") Long sucursalId,
        @RequestParam Boolean activa) {

        log.info("PATCH /api/familias-producto/{}/estado - empresaId: {}, sucursalId: {}, activa: {}",
            id, empresaId, sucursalId, activa);

        FamiliaProductoDTO familia = familiaService.cambiarEstado(id, activa, empresaId, sucursalId);

        return ResponseEntity.ok(ApiResponse.<FamiliaProductoDTO>builder()
            .success(true)
            .message("Estado de familia actualizado exitosamente")
            .data(familia)
            .build());
    }
}