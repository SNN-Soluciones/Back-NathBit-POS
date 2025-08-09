package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.auth.CambiarPasswordRequest;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.usuario.*;
import com.snnsoluciones.backnathbitpos.service.usuario.UsuarioGestionService;
import com.snnsoluciones.backnathbitpos.service.usuario.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de usuarios.
 * Maneja operaciones CRUD y gestión de roles.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Slf4j
public class UsuarioController {
    
    private final UsuarioService usuarioService;
    private final UsuarioGestionService usuarioGestionService;
    
    /**
     * Obtiene el perfil del usuario actual.
     */
    @GetMapping("/perfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UsuarioDTO>> obtenerPerfil(Authentication authentication) {
        try {
            Long usuarioId = Long.parseLong(authentication.getName());
            UsuarioDTO usuario = usuarioService.findById(usuarioId);
            return ResponseEntity.ok(
                ApiResponse.success("Perfil obtenido exitosamente", usuario)
            );
        } catch (Exception e) {
            log.error("Error al obtener perfil: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Actualiza el perfil del usuario actual.
     */
    @PutMapping("/perfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UsuarioDTO>> actualizarPerfil(
            Authentication authentication,
            @Valid @RequestBody ActualizarUsuarioRequest request) {
        try {
            Long usuarioId = Long.parseLong(authentication.getName());
            UsuarioDTO usuario = usuarioGestionService.actualizarUsuario(usuarioId, request);
            return ResponseEntity.ok(
                ApiResponse.success("Perfil actualizado exitosamente", usuario)
            );
        } catch (Exception e) {
            log.error("Error al actualizar perfil: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Lista usuarios con filtros y paginación.
     * Solo para administradores.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<Page<UsuarioDTO>>> listarUsuarios(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        
        log.info("Listando usuarios - Empresa: {}, Sucursal: {}", empresaId, sucursalId);
        
        try {
            Page<UsuarioDTO> usuarios = usuarioGestionService.listarUsuarios(
                empresaId, sucursalId, rol, activo, search, pageable
            );
            return ResponseEntity.ok(
                ApiResponse.success("Usuarios obtenidos exitosamente", usuarios)
            );
        } catch (Exception e) {
            log.error("Error al listar usuarios: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Crea un nuevo usuario.
     * Solo para administradores.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<UsuarioDTO>> crearUsuario(
            @Valid @RequestBody CrearUsuarioRequest request) {
        log.info("Creando usuario: {}", request.getEmail());
        
        try {
            UsuarioDTO usuario = usuarioGestionService.crearUsuario(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Usuario creado exitosamente", usuario));
        } catch (Exception e) {
            log.error("Error al crear usuario: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Obtiene un usuario por ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<UsuarioDTO>> obtenerUsuario(@PathVariable Long id) {
        try {
            UsuarioDTO usuario = usuarioService.findById(id);
            return ResponseEntity.ok(
                ApiResponse.success("Usuario obtenido exitosamente", usuario)
            );
        } catch (Exception e) {
            log.error("Error al obtener usuario: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Actualiza un usuario existente.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<UsuarioDTO>> actualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarUsuarioRequest request) {
        log.info("Actualizando usuario ID: {}", id);
        
        try {
            UsuarioDTO usuario = usuarioGestionService.actualizarUsuario(id, request);
            return ResponseEntity.ok(
                ApiResponse.success("Usuario actualizado exitosamente", usuario)
            );
        } catch (Exception e) {
            log.error("Error al actualizar usuario: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Activa o desactiva un usuario.
     */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<UsuarioDTO>> cambiarEstadoUsuario(
            @PathVariable Long id,
            @RequestParam boolean activo) {
        log.info("Cambiando estado de usuario ID: {} a {}", id, activo ? "activo" : "inactivo");
        
        try {
            UsuarioDTO usuario = usuarioService.cambiarEstadoUsuario(id, activo);
            return ResponseEntity.ok(
                ApiResponse.success(
                    activo ? "Usuario activado exitosamente" : "Usuario desactivado exitosamente", 
                    usuario
                )
            );
        } catch (Exception e) {
            log.error("Error al cambiar estado de usuario: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Obtiene los roles asignados a un usuario.
     */
    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<List<UsuarioEmpresaRolDTO>>> obtenerRolesUsuario(
            @PathVariable Long id) {
        try {
            List<UsuarioEmpresaRolDTO> roles = usuarioGestionService.obtenerRolesUsuario(id);
            return ResponseEntity.ok(
                ApiResponse.success("Roles obtenidos exitosamente", roles)
            );
        } catch (Exception e) {
            log.error("Error al obtener roles de usuario: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Asigna un nuevo rol a un usuario.
     */
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<UsuarioEmpresaRolDTO>> asignarRol(
            @PathVariable Long id,
            @Valid @RequestBody AsignarRolRequest request) {
        log.info("Asignando rol a usuario ID: {}", id);
        
        try {
            UsuarioEmpresaRolDTO rol = usuarioGestionService.asignarRol(id, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Rol asignado exitosamente", rol));
        } catch (Exception e) {
            log.error("Error al asignar rol: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Remueve un rol de un usuario.
     */
    @DeleteMapping("/{usuarioId}/roles/{rolId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<Void>> removerRol(
            @PathVariable Long usuarioId,
            @PathVariable Long rolId) {
        log.info("Removiendo rol ID: {} de usuario ID: {}", rolId, usuarioId);
        
        try {
            usuarioGestionService.removerRol(usuarioId, rolId);
            return ResponseEntity.ok(
                ApiResponse.success("Rol removido exitosamente", null)
            );
        } catch (Exception e) {
            log.error("Error al remover rol: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Obtiene los accesos disponibles del usuario actual.
     */
    @GetMapping("/mis-accesos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AccesoDTO>>> obtenerMisAccesos(
            Authentication authentication) {
        try {
            Long usuarioId = Long.parseLong(authentication.getName());
            List<AccesoDTO> accesos = usuarioService.obtenerAccesos(usuarioId);
            return ResponseEntity.ok(
                ApiResponse.success("Accesos obtenidos exitosamente", accesos)
            );
        } catch (Exception e) {
            log.error("Error al obtener accesos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Cambia la contraseña del usuario actual.
     */
    @PostMapping("/cambiar-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> cambiarPassword(
            Authentication authentication,
            @Valid @RequestBody CambiarPasswordRequest request) {
        try {
            Long usuarioId = Long.parseLong(authentication.getName());
            usuarioService.cambiarPassword(
                usuarioId, 
                request.getPasswordActual(), 
                request.getPasswordNueva()
            );
            return ResponseEntity.ok(
                ApiResponse.success("Contraseña cambiada exitosamente", null)
            );
        } catch (Exception e) {
            log.error("Error al cambiar contraseña: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
}