package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.usuarios.*;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresa;
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
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gestión de usuarios")
public class UsuarioController extends BaseController { // CAMBIO 1: Extender BaseController

    private final UsuarioService usuarioService;
    private final UsuarioEmpresaService usuarioEmpresaService;

    @Operation(summary = "Listar todos los usuarios")
    @GetMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> listar() {
        // CAMBIO 2: Filtrar por contexto si no es rol sistema
        List<Usuario> usuarios;

        if (isRolSistema()) {
            // ROOT y SOPORTE ven todos
            usuarios = usuarioService.listarTodos();
        } else {
            // Otros roles ven solo usuarios de su empresa
            Long empresaId = getCurrentEmpresaId();
            if (empresaId == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Debe seleccionar una empresa"));
            }
            usuarios = usuarioService.listarPorEmpresa(empresaId);
        }

        List<UsuarioResponse> response = usuarios.stream()
            .map(this::convertirAResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Obtener usuario por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioResponse>> obtenerPorId(@PathVariable Long id) {
        Usuario usuario = usuarioService.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // CAMBIO 3: Verificar acceso si no es rol sistema
        if (!isRolSistema()) {
            Long empresaId = getCurrentEmpresaId();
            boolean tieneAcceso = usuarioEmpresaService.listarPorUsuario(id).stream()
                .anyMatch(ue -> ue.getEmpresa().getId().equals(empresaId));

            if (!tieneAcceso) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tienes acceso a este usuario"));
            }
        }

        return ResponseEntity.ok(ApiResponse.ok(convertirAResponse(usuario)));
    }

    @Operation(summary = "Crear nuevo usuario")
    @PostMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioResponse>> crear(
        @Valid @RequestBody UsuarioRequest request) {

        // Validar email único
        if (usuarioService.existeEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("El email ya existe"));
        }

        Usuario usuario = convertirAEntity(request);
        Usuario nuevoUsuario = usuarioService.crear(usuario);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Usuario creado", convertirAResponse(nuevoUsuario)));
    }

    @Operation(summary = "Actualizar usuario")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizar(
        @PathVariable Long id,
        @Valid @RequestBody UsuarioUpdateRequest request) {

        // CAMBIO 4: Verificar acceso antes de actualizar
        if (!isRolSistema()) {
            Long empresaId = getCurrentEmpresaId();
            boolean tieneAcceso = usuarioEmpresaService.listarPorUsuario(id).stream()
                .anyMatch(ue -> ue.getEmpresa().getId().equals(empresaId));

            if (!tieneAcceso) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tienes acceso a este usuario"));
            }
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail());
        usuario.setPassword(request.getPassword());
        usuario.setNombre(request.getNombre());
        usuario.setApellidos(request.getApellidos());
        usuario.setRol(request.getRol());
        usuario.setActivo(request.getActivo());

        Usuario actualizado = usuarioService.actualizar(id, usuario);

        return ResponseEntity.ok(ApiResponse.ok("Usuario actualizado", convertirAResponse(actualizado)));
    }

    @Operation(summary = "Eliminar usuario")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Usuario eliminado", null));
    }

    // === ASIGNACIONES EMPRESA/SUCURSAL ===

    @Operation(summary = "Asignar usuario a empresa/sucursal")
    @PostMapping("/asignar")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioEmpresaResponse>> asignarEmpresa(
        @Valid @RequestBody UsuarioEmpresaRequest request) {

        // CAMBIO 5: Si no es rol sistema, solo puede asignar a su empresa
        if (!isRolSistema()) {
            Long empresaId = getCurrentEmpresaId();
            if (!request.getEmpresaId().equals(empresaId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Solo puedes asignar usuarios a tu empresa"));
            }
        }

        UsuarioEmpresa asignacion = usuarioEmpresaService.asignar(
            request.getUsuarioId(),
            request.getEmpresaId(),
            request.getSucursalId()
        );

        return ResponseEntity.ok(ApiResponse.ok("Asignación creada", convertirAEmpresaResponse(asignacion)));
    }

    @Operation(summary = "Listar asignaciones de un usuario")
    @GetMapping("/{id}/asignaciones")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<UsuarioEmpresaResponse>>> listarAsignaciones(
        @PathVariable Long id) {

        List<UsuarioEmpresa> asignaciones = usuarioEmpresaService.listarPorUsuario(id);

        // CAMBIO 6: Filtrar asignaciones si no es rol sistema
        if (!isRolSistema()) {
            Long empresaId = getCurrentEmpresaId();
            asignaciones = asignaciones.stream()
                .filter(a -> a.getEmpresa().getId().equals(empresaId))
                .collect(Collectors.toList());
        }

        List<UsuarioEmpresaResponse> response = asignaciones.stream()
            .map(this::convertirAEmpresaResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Eliminar asignación")
    @DeleteMapping("/asignacion/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> desasignar(@PathVariable Long id) {
        // CAMBIO 7: Verificar que la asignación pertenece a la empresa del contexto
        if (!isRolSistema()) {
            UsuarioEmpresa asignacion = usuarioEmpresaService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Asignación no encontrada"));

            Long empresaId = getCurrentEmpresaId();
            if (!asignacion.getEmpresa().getId().equals(empresaId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tienes acceso a esta asignación"));
            }
        }

        usuarioEmpresaService.desasignar(id);
        return ResponseEntity.ok(ApiResponse.ok("Asignación eliminada", null));
    }

    // === MI PERFIL === (sin cambios, ya usa getCurrentUserId)

    @Operation(summary = "Obtener mi perfil")
    @GetMapping("/perfil")
    public ResponseEntity<ApiResponse<UsuarioResponse>> miPerfil() {
        // Ya usa getCurrentUserId() que viene de BaseController
        Long userId = getCurrentUserId();

        Usuario usuario = usuarioService.buscarPorId(userId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return ResponseEntity.ok(ApiResponse.ok(convertirAResponse(usuario)));
    }

    @Operation(summary = "Actualizar mi perfil")
    @PutMapping("/perfil")
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizarPerfil(
        @Valid @RequestBody UsuarioUpdateRequest request) {

        Long userId = getCurrentUserId();

        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setApellidos(request.getApellidos());
        // Email, rol y activo no se pueden cambiar desde perfil

        Usuario actualizado = usuarioService.actualizar(userId, usuario);

        return ResponseEntity.ok(ApiResponse.ok("Perfil actualizado", convertirAResponse(actualizado)));
    }

    // Métodos auxiliares (sin cambios)

    private UsuarioResponse convertirAResponse(Usuario usuario) {
        UsuarioResponse response = new UsuarioResponse();
        response.setId(usuario.getId());
        response.setEmail(usuario.getEmail());
        response.setNombre(usuario.getNombre());
        response.setApellidos(usuario.getApellidos());
        response.setRol(usuario.getRol());
        response.setActivo(usuario.getActivo());
        response.setCreatedAt(usuario.getCreatedAt());
        response.setUpdatedAt(usuario.getUpdatedAt());

        // Agregar empresas asignadas
        List<UsuarioEmpresa> asignaciones = usuarioEmpresaService.listarPorUsuario(usuario.getId());
        response.setEmpresas(asignaciones.stream()
            .map(ue -> ue.getEmpresa().getNombreRazonSocial())
            .collect(Collectors.toList()));

        response.setSucursales(asignaciones.stream()
            .filter(ue -> ue.getSucursal() != null)
            .map(ue -> ue.getSucursal().getNombre())
            .distinct()
            .collect(Collectors.toList()));

        return response;
    }

    private Usuario convertirAEntity(UsuarioRequest request) {
        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail());
        usuario.setPassword(request.getPassword());
        usuario.setNombre(request.getNombre());
        usuario.setApellidos(request.getApellidos());
        usuario.setRol(request.getRol());
        return usuario;
    }

    private UsuarioEmpresaResponse convertirAEmpresaResponse(UsuarioEmpresa asignacion) {
        UsuarioEmpresaResponse response = new UsuarioEmpresaResponse();
        response.setId(asignacion.getId());
        response.setUsuarioId(asignacion.getUsuario().getId());
        response.setEmpresaId(asignacion.getEmpresa().getId());
        response.setEmpresaNombre(asignacion.getEmpresa().getNombreRazonSocial());

        if (asignacion.getSucursal() != null) {
            response.setSucursalId(asignacion.getSucursal().getId());
            response.setSucursalNombre(asignacion.getSucursal().getNombre());
        }

        response.setFechaAsignacion(asignacion.getFechaAsignacion());
        response.setActivo(asignacion.getActivo());

        return response;
    }
}