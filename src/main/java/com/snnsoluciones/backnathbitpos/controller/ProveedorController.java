package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.proveedor.ProveedorDto;
import com.snnsoluciones.backnathbitpos.dto.proveedor.ProveedorRequest;
import com.snnsoluciones.backnathbitpos.service.ProveedorService;
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

@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
@Tag(name = "Proveedores", description = "Gestión de proveedores")
@Slf4j
public class ProveedorController {

    private final ProveedorService proveedorService;

    @Operation(summary = "Listar proveedores por empresa")
    @GetMapping("/empresa/{empresaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'CAJERO')")
    public ResponseEntity<ApiResponse<List<ProveedorDto>>> listarPorEmpresa(
            @PathVariable Long empresaId,
            @RequestParam(required = false) String busqueda) {
        
        List<ProveedorDto> proveedores = proveedorService.listarPorEmpresa(empresaId, busqueda);
        return ResponseEntity.ok(ApiResponse.ok(
                "Proveedores encontrados: " + proveedores.size(), 
                proveedores
        ));
    }

    @Operation(summary = "Obtener proveedor por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProveedorDto>> obtenerPorId(@PathVariable Long id) {
        
        ProveedorDto proveedor = proveedorService.obtenerPorId(id);
        if (proveedor == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Proveedor no encontrado"));
        }
        
        return ResponseEntity.ok(ApiResponse.ok(proveedor));
    }

    @Operation(summary = "Crear proveedor")
    @PostMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProveedorDto>> crear(
            @Valid @RequestBody ProveedorRequest request) {
        
        log.info("Creando proveedor: {} para empresa: {}", 
                request.getNombreComercial(), request.getEmpresaId());
        
        try {
            ProveedorDto proveedor = proveedorService.crear(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Proveedor creado exitosamente", proveedor));
                    
        } catch (Exception e) {
            log.error("Error al crear proveedor: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Actualizar proveedor")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProveedorDto>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProveedorRequest request) {
        
        log.info("Actualizando proveedor ID: {}", id);
        
        try {
            ProveedorDto proveedor = proveedorService.actualizar(id, request);
            return ResponseEntity.ok(ApiResponse.ok(
                    "Proveedor actualizado exitosamente", 
                    proveedor
            ));
            
        } catch (Exception e) {
            log.error("Error al actualizar proveedor: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Activar/Desactivar proveedor")
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProveedorDto>> toggleActivo(@PathVariable Long id) {
        
        try {
            ProveedorDto proveedor = proveedorService.toggleActivo(id);
            String mensaje = proveedor.getActivo() ? 
                    "Proveedor activado" : "Proveedor desactivado";
            
            return ResponseEntity.ok(ApiResponse.ok(mensaje, proveedor));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Eliminar proveedor")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> eliminar(@PathVariable Long id) {
        
        try {
            proveedorService.eliminar(id);
            return ResponseEntity.ok(ApiResponse.ok("Proveedor eliminado"));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Buscar por identificación")
    @GetMapping("/buscar-identificacion")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'CAJERO')")
    public ResponseEntity<ApiResponse<ProveedorDto>> buscarPorIdentificacion(
            @RequestParam Long empresaId,
            @RequestParam String identificacion) {
        
        ProveedorDto proveedor = proveedorService.buscarPorIdentificacion(empresaId, identificacion);
        if (proveedor == null) {
            return ResponseEntity.ok(ApiResponse.ok("Proveedor no encontrado", null));
        }
        
        return ResponseEntity.ok(ApiResponse.ok(proveedor));
    }
}