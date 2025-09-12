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
public class UsuarioController extends BaseController {

    private final UsuarioService usuarioService;
    private final UsuarioEmpresaService usuarioEmpresaService;

    @Operation(summary = "Listar todos los usuarios")
    @GetMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> listar() {
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

        // Verificar acceso si no es rol sistema
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

    @Operation(summary = "Actualizar usuario",
        description = "Solo se puede actualizar nombre, apellidos y estado. Email y rol no se pueden cambiar.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizar(
        @PathVariable Long id,
        @Valid @RequestBody UsuarioUpdateRequest request) {

        // Verificar acceso antes de actualizar
        if (!isRolSistema()) {
            Long empresaId = getCurrentEmpresaId();
            boolean tieneAcceso = usuarioEmpresaService.listarPorUsuario(id).stream()
                .anyMatch(ue -> ue.getEmpresa().getId().equals(empresaId));

            if (!tieneAcceso) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tienes acceso a este usuario"));
            }
        }

        // Solo actualizar campos permitidos
        Usuario usuarioExistente = usuarioService.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuarioExistente.setNombre(request.getNombre());
        usuarioExistente.setApellidos(request.getApellidos());
        usuarioExistente.setTelefono(request.getTelefono());
        usuarioExistente.setActivo(request.getActivo());

        // Si se proporciona nueva contraseña
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            usuarioExistente.setPassword(request.getPassword()); // El servicio la encriptará
        }

        Usuario actualizado = usuarioService.actualizar(id, usuarioExistente);

        return ResponseEntity.ok(ApiResponse.ok("Usuario actualizado", convertirAResponse(actualizado)));
    }

    @Operation(summary = "Eliminar usuario (desactivar)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        Usuario usuario = usuarioService.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setActivo(false);
        usuarioService.actualizar(id, usuario);

        return ResponseEntity.ok(ApiResponse.ok("Usuario desactivado", null));
    }

    @Operation(summary = "Cambiar contraseña de usuario")
    @PutMapping("/{id}/cambiar-password")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cambiarPassword(
        @PathVariable Long id,
        @Valid @RequestBody CambiarPasswordRequest request) {

        // Verificar acceso
        if (!isRolSistema()) {
            Long empresaId = getCurrentEmpresaId();
            boolean tieneAcceso = usuarioEmpresaService.listarPorUsuario(id).stream()
                .anyMatch(ue -> ue.getEmpresa().getId().equals(empresaId));

            if (!tieneAcceso) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tienes acceso a este usuario"));
            }
        }

        usuarioService.cambiarPassword(id, request.getPassword());
        return ResponseEntity.ok(ApiResponse.ok("Contraseña actualizada", null));
    }

    // === MI PERFIL ===

    @Operation(summary = "Obtener mi perfil")
    @GetMapping("/perfil")
    public ResponseEntity<ApiResponse<UsuarioResponse>> miPerfil() {
        Long userId = getCurrentUserId();

        Usuario usuario = usuarioService.buscarPorId(userId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return ResponseEntity.ok(ApiResponse.ok(convertirAResponse(usuario)));
    }

    @Operation(summary = "Actualizar mi perfil")
    @PutMapping("/perfil")
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizarPerfil(
        @Valid @RequestBody PerfilUpdateRequest request) {

        Long userId = getCurrentUserId();
        Usuario usuario = usuarioService.buscarPorId(userId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Solo actualizar campos permitidos en perfil
        usuario.setNombre(request.getNombre());
        usuario.setApellidos(request.getApellidos());
        usuario.setTelefono(request.getTelefono());

        Usuario actualizado = usuarioService.actualizar(userId, usuario);

        return ResponseEntity.ok(ApiResponse.ok("Perfil actualizado", convertirAResponse(actualizado)));
    }

    @Operation(summary = "Cambiar mi contraseña")
    @PutMapping("/perfil/cambiar-password")
    public ResponseEntity<ApiResponse<Void>> cambiarMiPassword(
        @Valid @RequestBody CambiarMiPasswordRequest request) {

        Long userId = getCurrentUserId();

        // Verificar contraseña actual
        if (!usuarioService.verificarPassword(userId, request.getPasswordActual())) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("La contraseña actual es incorrecta"));
        }

        usuarioService.cambiarPassword(userId, request.getPasswordNueva());
        return ResponseEntity.ok(ApiResponse.ok("Contraseña actualizada", null));
    }

    // Métodos auxiliares
    private UsuarioResponse convertirAResponse(Usuario usuario) {
        UsuarioResponse response = new UsuarioResponse();
        response.setId(usuario.getId());
        response.setEmail(usuario.getEmail());
        response.setNombre(usuario.getNombre());
        response.setApellidos(usuario.getApellidos());
        response.setTelefono(usuario.getTelefono());
        response.setRol(usuario.getRol());
        response.setActivo(usuario.getActivo());
        response.setCreatedAt(usuario.getCreatedAt());
        response.setUpdatedAt(usuario.getUpdatedAt());

        // Agregar empresas asignadas
        List<String> empresas = usuarioEmpresaService.listarPorUsuario(usuario.getId())
            .stream()
            .map(ue -> ue.getEmpresa().getNombreComercial())
            .collect(Collectors.toList());
        response.setEmpresas(empresas);

        return response;
    }
}