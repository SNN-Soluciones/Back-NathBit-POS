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
public class UsuarioController {
    
    private final UsuarioService usuarioService;
    private final UsuarioEmpresaService usuarioEmpresaService;
    
    @Operation(summary = "Listar todos los usuarios")
    @GetMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> listar() {
        List<Usuario> usuarios = usuarioService.listarTodos();
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
        List<UsuarioEmpresaResponse> response = asignaciones.stream()
            .map(this::convertirAEmpresaResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
    
    @Operation(summary = "Eliminar asignación")
    @DeleteMapping("/asignacion/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> desasignar(@PathVariable Long id) {
        usuarioEmpresaService.desasignar(id);
        return ResponseEntity.ok(ApiResponse.ok("Asignación eliminada", null));
    }
    
    // === MI PERFIL ===
    
    @Operation(summary = "Obtener mi perfil")
    @GetMapping("/perfil")
    public ResponseEntity<ApiResponse<UsuarioResponse>> miPerfil() {
        // Obtener ID del usuario actual del token
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
    
    // Métodos de conversión
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
        return response;
    }
    
    private Usuario convertirAEntity(UsuarioRequest request) {
        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail());
        usuario.setPassword(request.getPassword());
        usuario.setNombre(request.getNombre());
        usuario.setApellidos(request.getApellidos());
        usuario.setRol(request.getRol());
        usuario.setActivo(request.getActivo());
        return usuario;
    }
    
    private UsuarioEmpresaResponse convertirAEmpresaResponse(UsuarioEmpresa ue) {
        UsuarioEmpresaResponse response = new UsuarioEmpresaResponse();
        response.setId(ue.getId());
        response.setUsuarioId(ue.getUsuario().getId());
        response.setUsuarioNombre(ue.getUsuario().getNombre());
        response.setUsuarioEmail(ue.getUsuario().getEmail());
        response.setEmpresaId(ue.getEmpresa().getId());
        response.setEmpresaNombre(ue.getEmpresa().getNombre());
        
        if (ue.getSucursal() != null) {
            response.setSucursalId(ue.getSucursal().getId());
            response.setSucursalNombre(ue.getSucursal().getNombre());
        }
        
        response.setFechaAsignacion(ue.getFechaAsignacion());
        response.setActivo(ue.getActivo());
        return response;
    }
    
    private Long getCurrentUserId() {
        return (Long) org.springframework.security.core.context.SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();
    }
}