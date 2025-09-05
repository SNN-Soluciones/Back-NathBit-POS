// src/main/java/com/snnsoluciones/backnathbitpos/controller/EmpresaController.java
package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.common.PageResponse;
import com.snnsoluciones.backnathbitpos.dto.empresa.EmpresaRequest;
import com.snnsoluciones.backnathbitpos.dto.empresa.EmpresaResponse;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.service.EmpresaService;
import com.snnsoluciones.backnathbitpos.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
@Tag(name = "Empresas", description = "Consulta y gestión de empresas")
@Slf4j
public class EmpresaController extends BaseController{

    private final EmpresaService empresaService;
    private final UsuarioService usuarioService;

    @Operation(summary = "Listar todas las empresas (ROOT/SOPORTE)")
    @GetMapping("/todas")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE')")
    public ResponseEntity<ApiResponse<List<EmpresaResponse>>> listarTodas() {
        List<Empresa> empresas = empresaService.listarTodas();
        List<EmpresaResponse> response = empresas.stream()
            .map(this::convertirAResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Obtener empresa por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EmpresaResponse>> obtenerPorId(@PathVariable Long id) {
        Empresa empresa = empresaService.buscarPorId(id);
        if (empresa == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Empresa no encontrada"));
        }

        // Validar acceso para SUPER_ADMIN
        if (!validarAccesoEmpresa(empresa)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("No tiene acceso a esta empresa"));
        }

        return ResponseEntity.ok(ApiResponse.ok(convertirAResponse(empresa)));
    }

    @Operation(summary = "Actualizar empresa")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EmpresaResponse>> actualizar(
        @PathVariable Long id,
        @Valid @RequestBody EmpresaRequest request) {

        // Verificar que existe
        Empresa empresaExistente = empresaService.buscarPorId(id);
        if (empresaExistente == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Empresa no encontrada"));
        }

        // Validar acceso
        if (!validarAccesoEmpresa(empresaExistente)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("No tiene acceso a esta empresa"));
        }

        Empresa empresa = convertirAEntity(request);
        Empresa actualizada = empresaService.actualizar(id, empresa);

        return ResponseEntity.ok(
            ApiResponse.ok("Empresa actualizada correctamente", convertirAResponse(actualizada))
        );
    }

    @Operation(summary = "Eliminar empresa (soft delete)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        empresaService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Empresa eliminada correctamente", null));
    }

    @Operation(summary = "Listar empresas del usuario actual")
    @GetMapping("/mis-empresas")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<EmpresaResponse>>> listarMisEmpresas(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size,
        @RequestParam(name = "sortBy", defaultValue = "nombreComercial") String sortBy,
        @RequestParam(name = "sortDirection", defaultValue = "ASC") String sortDirection) {

        // CAMBIO 2: Usar getCurrentUserId() heredado de BaseController
        Long usuarioId = getCurrentUserId(); // Ya no necesita parsear, viene del BaseController

        Sort.Direction direction = sortDirection.equalsIgnoreCase("DESC")
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Empresa> empresasPage = empresaService.listarPorUsuarioPaginado(usuarioId, pageable);
        Page<EmpresaResponse> responsePage = empresasPage.map(this::convertirAResponse);

        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(responsePage)));
    }

    @Operation(summary = "Cambiar estado de empresa (activar/desactivar)")
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EmpresaResponse>> cambiarEstado(
        @PathVariable Long id,
        @RequestParam Boolean activa) {

        Empresa empresa = empresaService.buscarPorId(id);
        if (empresa == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Empresa no encontrada"));
        }

        // Validar acceso
        if (!validarAccesoEmpresa(empresa)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("No tiene acceso a esta empresa"));
        }

        empresa.setActiva(activa);
        Empresa actualizada = empresaService.actualizar(id, empresa);

        String mensaje = activa ? "Empresa activada correctamente" : "Empresa desactivada correctamente";
        return ResponseEntity.ok(ApiResponse.ok(mensaje, convertirAResponse(actualizada)));
    }

    @Operation(summary = "Verificar si existe una identificación")
    @GetMapping("/existe-identificacion")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> existeIdentificacion(
        @RequestParam String identificacion) {

        boolean existe = empresaService.existeIdentificacion(identificacion);
        return ResponseEntity.ok(ApiResponse.ok(existe));
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private boolean validarAccesoEmpresa(Empresa empresa) {
        // Usar isRolSistema() heredado de BaseController
        if (isRolSistema()) {
            return true;
        }

        // Para SUPER_ADMIN, verificar acceso a la empresa
        if ("SUPER_ADMIN".equals(getCurrentUserRole())) {
            Long usuarioId = getCurrentUserId();
            return empresaService.usuarioTieneAcceso(usuarioId, empresa.getId());
        }

        return false;
    }

    private EmpresaResponse convertirAResponse(Empresa empresa) {
        return EmpresaResponse.builder()
            .id(empresa.getId())
            .nombreRazonSocial(empresa.getNombreRazonSocial())
            .nombreComercial(empresa.getNombreComercial())
            .tipoIdentificacion(empresa.getTipoIdentificacion())
            .identificacion(empresa.getIdentificacion())
            .email(empresa.getEmail())
            .telefono(empresa.getTelefono())
            .requiereHacienda(empresa.getRequiereHacienda() != null && empresa.getRequiereHacienda())
            .activa(empresa.getActiva())
            .createdAt(empresa.getCreatedAt())
            .updatedAt(empresa.getUpdatedAt())
            .build();
    }

    private Empresa convertirAEntity(EmpresaRequest request) {
        Empresa empresa = new Empresa();
        empresa.setNombreRazonSocial(request.getNombreComercial());
        empresa.setNombreComercial(request.getNombreComercial());
        empresa.setTipoIdentificacion(request.getTipoIdentificacion());
        empresa.setIdentificacion(request.getIdentificacion());
        empresa.setEmail(request.getEmail());
        empresa.setTelefono(request.getTelefono());
        empresa.setActiva(request.getActiva());
        return empresa;
    }
}