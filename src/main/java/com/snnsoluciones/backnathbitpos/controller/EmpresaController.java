package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.empresa.EmpresaRequest;
import com.snnsoluciones.backnathbitpos.dto.empresa.EmpresaResponse;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.service.EmpresaService;
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
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
@Tag(name = "Empresas", description = "Gestión de empresas")
public class EmpresaController {

    private final EmpresaService empresaService;
    private final UsuarioService usuarioService;

    @Operation(summary = "Listar todas las empresas")
    @GetMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE')")
    public ResponseEntity<ApiResponse<List<EmpresaResponse>>> listar() {
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
        Empresa empresa = empresaService.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        return ResponseEntity.ok(ApiResponse.ok(convertirAResponse(empresa)));
    }

    @Operation(summary = "Crear nueva empresa")
    @PostMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EmpresaResponse>> crear(
        @Valid @RequestBody EmpresaRequest request) {

        // Validar código único
        if (empresaService.existeCodigo(request.getCodigo())) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("El código ya existe"));
        }

        Empresa empresa = convertirAEntity(request);
        Empresa nuevaEmpresa = empresaService.crear(empresa);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Empresa creada", convertirAResponse(nuevaEmpresa)));
    }

    @Operation(summary = "Actualizar empresa")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EmpresaResponse>> actualizar(
        @PathVariable Long id,
        @Valid @RequestBody EmpresaRequest request) {

        Empresa empresa = convertirAEntity(request);
        Empresa actualizada = empresaService.actualizar(id, empresa);

        return ResponseEntity.ok(ApiResponse.ok("Empresa actualizada", convertirAResponse(actualizada)));
    }

    @Operation(summary = "Eliminar empresa")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        empresaService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Empresa eliminada", null));
    }

    @Operation(summary = "Listar empresas accesibles por usuario")
    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<EmpresaResponse>>> listarPorUsuario(
        @PathVariable(name = "usuarioId") Long usuarioId) {
        // Validación simple: solo puedes ver tus propias empresas (excepto ROOT/SOPORTE)
        Long usuarioActualId = getCurrentUserId();
        Usuario usuarioActual = usuarioService.buscarPorId(usuarioActualId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!usuarioActual.esRolSistema() && !usuarioActualId.equals(usuarioId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Solo puede ver sus propias empresas"));
        }

        List<Empresa> empresas = empresaService.listarPorUsuario(usuarioId);
        List<EmpresaResponse> response = empresas.stream()
            .map(this::convertirAResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // Métodos de conversión
    private EmpresaResponse convertirAResponse(Empresa empresa) {
        EmpresaResponse response = new EmpresaResponse();
        response.setId(empresa.getId());
        response.setNombre(empresa.getNombre());
        response.setCodigo(empresa.getCodigo());
        response.setTipoIdentificacion(empresa.getTipoIdentificacion());
        response.setIdentificacion(empresa.getIdentificacion());
        response.setDireccion(empresa.getDireccion());
        response.setTelefono(empresa.getTelefono());
        response.setEmail(empresa.getEmail());
        response.setActiva(empresa.getActiva());
        response.setCreatedAt(empresa.getCreatedAt());
        response.setUpdatedAt(empresa.getUpdatedAt());
        return response;
    }

    private Empresa convertirAEntity(EmpresaRequest request) {
        Empresa empresa = new Empresa();
        empresa.setNombre(request.getNombre());
        empresa.setCodigo(request.getCodigo());
        empresa.setTipoIdentificacion(request.getTipoIdentificacion());
        empresa.setIdentificacion(request.getIdentificacion());
        empresa.setDireccion(request.getDireccion());
        empresa.setTelefono(request.getTelefono());
        empresa.setEmail(request.getEmail());
        empresa.setActiva(request.getActiva());
        return empresa;
    }

    private Long getCurrentUserId() {
        return (Long) org.springframework.security.core.context.SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();
    }
}