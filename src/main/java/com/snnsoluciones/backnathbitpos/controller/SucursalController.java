package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.sucursal.SucursalRequest;
import com.snnsoluciones.backnathbitpos.dto.sucursal.SucursalResponse;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.service.EmpresaService;
import com.snnsoluciones.backnathbitpos.service.SucursalService;
import com.snnsoluciones.backnathbitpos.service.UsuarioEmpresaService;
import com.snnsoluciones.backnathbitpos.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sucursales")
@RequiredArgsConstructor
@Tag(name = "Sucursales", description = "Gestión de sucursales")
public class SucursalController {
    
    private final SucursalService sucursalService;
    private final EmpresaService empresaService;
    private final UsuarioService usuarioService;
    private final UsuarioEmpresaService usuarioEmpresaService;
    
    @Operation(summary = "Listar sucursales por empresa")
    @GetMapping("/empresa/{empresaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<SucursalResponse>>> listarPorEmpresa(
            @PathVariable Long empresaId) {
        
        List<Sucursal> sucursales = sucursalService.listarPorEmpresa(empresaId);
        List<SucursalResponse> response = sucursales.stream()
            .map(this::convertirAResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
    
    @Operation(summary = "Obtener sucursal por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<SucursalResponse>> obtenerPorId(@PathVariable Long id) {
        Sucursal sucursal = sucursalService.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));
        
        return ResponseEntity.ok(ApiResponse.ok(convertirAResponse(sucursal)));
    }
    
    @Operation(summary = "Crear nueva sucursal")
    @PostMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SucursalResponse>> crear(
            @Valid @RequestBody SucursalRequest request) {
        
        // Validar código único
        if (sucursalService.existeCodigo(request.getCodigo())) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("El código ya existe"));
        }
        
        Sucursal sucursal = convertirAEntity(request);
        Sucursal nuevaSucursal = sucursalService.crear(sucursal);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Sucursal creada", convertirAResponse(nuevaSucursal)));
    }
    
    @Operation(summary = "Actualizar sucursal")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SucursalResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody SucursalRequest request) {
        
        Sucursal sucursal = convertirAEntity(request);
        Sucursal actualizada = sucursalService.actualizar(id, sucursal);
        
        return ResponseEntity.ok(ApiResponse.ok("Sucursal actualizada", convertirAResponse(actualizada)));
    }
    
    @Operation(summary = "Eliminar sucursal")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        sucursalService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Sucursal eliminada", null));
    }

    @Operation(summary = "Listar sucursales de una empresa accesibles por usuario")
    @GetMapping("/usuario/{usuarioId}/empresa/{empresaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<SucursalResponse>>> listarPorUsuarioYEmpresa(
        @PathVariable(name = "usuarioId") Long usuarioId,
        @PathVariable(name = "empresaId") Long empresaId) {

        // Validación de seguridad
        Long usuarioActualId = getCurrentUserId();
        Usuario usuarioActual = usuarioService.buscarPorId(usuarioActualId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Si no es ROOT/SOPORTE, validar que sea su propio usuario
        if (!usuarioActual.esRolSistema() && !usuarioActualId.equals(usuarioId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Solo puede ver sus propias sucursales"));
        }

        // Validar que el usuario tenga acceso a esa empresa
        if (!usuarioActual.esRolSistema()) {
            boolean tieneAccesoEmpresa = usuarioEmpresaService.tieneAcceso(usuarioId, empresaId, null);
            if (!tieneAccesoEmpresa) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tiene acceso a esta empresa"));
            }
        }

        List<Sucursal> sucursales = sucursalService.listarPorUsuarioYEmpresa(usuarioId, empresaId);
        List<SucursalResponse> response = sucursales.stream()
            .map(this::convertirAResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private Long getCurrentUserId() {
        return (Long) org.springframework.security.core.context.SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();
    }
    
    // Métodos de conversión
    private SucursalResponse convertirAResponse(Sucursal sucursal) {
        SucursalResponse response = new SucursalResponse();
        response.setId(sucursal.getId());
        response.setNombre(sucursal.getNombre());
        response.setCodigo(sucursal.getCodigo());
        response.setDireccion(sucursal.getDireccion());
        response.setTelefono(sucursal.getTelefono());
        response.setEmail(sucursal.getEmail());
        response.setActiva(sucursal.getActiva());
        response.setEmpresaId(sucursal.getEmpresa().getId());
        response.setEmpresaNombre(sucursal.getEmpresa().getNombre());
        response.setCreatedAt(sucursal.getCreatedAt());
        response.setUpdatedAt(sucursal.getUpdatedAt());
        return response;
    }
    
    private Sucursal convertirAEntity(SucursalRequest request) {
        Sucursal sucursal = new Sucursal();
        sucursal.setNombre(request.getNombre());
        sucursal.setCodigo(request.getCodigo());
        sucursal.setDireccion(request.getDireccion());
        sucursal.setTelefono(request.getTelefono());
        sucursal.setEmail(request.getEmail());
        sucursal.setActiva(request.getActiva());
        
        // Establecer empresa
        Empresa empresa = empresaService.buscarPorId(request.getEmpresaId())
            .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
        sucursal.setEmpresa(empresa);
        
        return sucursal;
    }
}