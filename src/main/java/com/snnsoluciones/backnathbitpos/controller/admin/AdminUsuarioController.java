package com.snnsoluciones.backnathbitpos.controller.admin;

import com.snnsoluciones.backnathbitpos.dto.request.AsignarCajasRequest;
import com.snnsoluciones.backnathbitpos.dto.request.AsignarSucursalesRequest;
import com.snnsoluciones.backnathbitpos.dto.request.CambiarRolRequest;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.response.AuditEventResponse;
import com.snnsoluciones.backnathbitpos.dto.response.UsuarioResponse;
import com.snnsoluciones.backnathbitpos.service.admin.UsuarioService;
import com.snnsoluciones.backnathbitpos.service.audit.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador para operaciones administrativas sobre usuarios.
 * Requiere permisos de ADMIN.
 */
@RestController
@RequestMapping("/api/admin/usuarios")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Usuarios", description = "Endpoints administrativos para gestión de usuarios")
@SecurityRequirement(name = "bearer-jwt")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsuarioController {

    private final UsuarioService usuarioService;
    private final AuditService auditService;

    /**
     * Cambia el rol de un usuario.
     */
    @PutMapping("/{id}/rol")
    @Operation(summary = "Cambiar rol de usuario", 
              description = "Permite cambiar el rol asignado a un usuario. Requiere permisos de ADMIN.")
    public ResponseEntity<ApiResponse<UsuarioResponse>> cambiarRol(
            @Parameter(description = "ID del usuario") @PathVariable UUID id,
            @Valid @RequestBody CambiarRolRequest request) {
        
        log.info("Cambiando rol de usuario {} a {}", id, request.getNuevoRol());
        
        UsuarioResponse usuario = usuarioService.cambiarRol(id, request.getNuevoRol());
        
        auditService.logUpdateEvent(
            "USUARIO", 
            id.toString(), 
            request.getRolAnterior(), 
            request.getNuevoRol(), 
            "Cambio de rol de usuario"
        );
        
        return ResponseEntity.ok(
            ApiResponse.<UsuarioResponse>builder()
                .success(true)
                .message("Rol actualizado exitosamente")
                .data(usuario)
                .build()
        );
    }

    /**
     * Asigna sucursales a un usuario.
     */
    @PostMapping("/{id}/sucursales")
    @Operation(summary = "Asignar sucursales a usuario", 
              description = "Asigna una o más sucursales a las que el usuario tendrá acceso.")
    public ResponseEntity<ApiResponse<UsuarioResponse>> asignarSucursales(
            @Parameter(description = "ID del usuario") @PathVariable UUID id,
            @Valid @RequestBody AsignarSucursalesRequest request) {
        
        log.info("Asignando {} sucursales a usuario {}", request.getSucursalIds().size(), id);
        
        UsuarioResponse usuario = usuarioService.asignarSucursales(id, request.getSucursalIds());
        
        auditService.logResourceEvent(
            "ASIGNAR_SUCURSALES", 
            "USUARIO", 
            id.toString(), 
            String.format("Se asignaron %d sucursales al usuario", request.getSucursalIds().size()),
            true
        );
        
        return ResponseEntity.ok(
            ApiResponse.<UsuarioResponse>builder()
                .success(true)
                .message("Sucursales asignadas exitosamente")
                .data(usuario)
                .build()
        );
    }

    /**
     * Asigna cajas a un usuario.
     */
    @PostMapping("/{id}/cajas")
    @Operation(summary = "Asignar cajas a usuario", 
              description = "Asigna una o más cajas a las que el usuario tendrá acceso.")
    public ResponseEntity<ApiResponse<UsuarioResponse>> asignarCajas(
            @Parameter(description = "ID del usuario") @PathVariable UUID id,
            @Valid @RequestBody AsignarCajasRequest request) {
        
        log.info("Asignando {} cajas a usuario {}", request.getCajaIds().size(), id);
        
        UsuarioResponse usuario = usuarioService.asignarCajas(id, request.getCajaIds());
        
        auditService.logResourceEvent(
            "ASIGNAR_CAJAS", 
            "USUARIO", 
            id.toString(), 
            String.format("Se asignaron %d cajas al usuario", request.getCajaIds().size()),
            true
        );
        
        return ResponseEntity.ok(
            ApiResponse.<UsuarioResponse>builder()
                .success(true)
                .message("Cajas asignadas exitosamente")
                .data(usuario)
                .build()
        );
    }

    /**
     * Desbloquea un usuario bloqueado.
     */
    @PutMapping("/{id}/desbloquear")
    @Operation(summary = "Desbloquear usuario", 
              description = "Desbloquea un usuario que fue bloqueado por intentos fallidos de login.")
    public ResponseEntity<ApiResponse<Void>> desbloquearUsuario(
            @Parameter(description = "ID del usuario") @PathVariable UUID id) {
        
        log.info("Desbloqueando usuario {}", id);
        
        usuarioService.desbloquearUsuario(id);
        
        auditService.logResourceEvent(
            "DESBLOQUEAR_USUARIO", 
            "USUARIO", 
            id.toString(), 
            "Usuario desbloqueado manualmente por administrador",
            true
        );
        
        return ResponseEntity.ok(
            ApiResponse.<Void>builder()
                .success(true)
                .message("Usuario desbloqueado exitosamente")
                .build()
        );
    }

    /**
     * Resetea el contador de intentos fallidos de un usuario.
     */
    @PutMapping("/{id}/resetear-intentos")
    @Operation(summary = "Resetear intentos fallidos", 
              description = "Resetea el contador de intentos fallidos de login de un usuario.")
    public ResponseEntity<ApiResponse<Void>> resetearIntentos(
            @Parameter(description = "ID del usuario") @PathVariable UUID id) {
        
        log.info("Reseteando intentos fallidos de usuario {}", id);
        
        usuarioService.resetearIntentos(id);
        
        auditService.logResourceEvent(
            "RESETEAR_INTENTOS", 
            "USUARIO", 
            id.toString(), 
            "Contador de intentos fallidos reseteado",
            true
        );
        
        return ResponseEntity.ok(
            ApiResponse.<Void>builder()
                .success(true)
                .message("Intentos reseteados exitosamente")
                .build()
        );
    }

    /**
     * Obtiene el historial de login de un usuario.
     */
    @GetMapping("/{id}/historial-login")
    @Operation(summary = "Historial de login", 
              description = "Obtiene el historial de intentos de login (exitosos y fallidos) de un usuario.")
    public ResponseEntity<ApiResponse<List<AuditEventResponse>>> obtenerHistorialLogin(
            @Parameter(description = "ID del usuario") @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Obteniendo historial de login para usuario {}", id);
        
        List<AuditEventResponse> historial = usuarioService.obtenerHistorialLogin(id, page, size);
        
        return ResponseEntity.ok(
            ApiResponse.<List<AuditEventResponse>>builder()
                .success(true)
                .message("Historial obtenido exitosamente")
                .data(historial)
                .build()
        );
    }

    /**
     * Fuerza el cierre de todas las sesiones activas de un usuario.
     */
    @PostMapping("/{id}/cerrar-sesiones")
    @Operation(summary = "Cerrar todas las sesiones", 
              description = "Invalida todos los tokens activos de un usuario, forzando el cierre de todas sus sesiones.")
    public ResponseEntity<ApiResponse<Void>> cerrarTodasLasSesiones(
            @Parameter(description = "ID del usuario") @PathVariable UUID id) {
        
        log.info("Cerrando todas las sesiones del usuario {}", id);
        
        usuarioService.cerrarTodasLasSesiones(id);
        
        auditService.logResourceEvent(
            "CERRAR_SESIONES", 
            "USUARIO", 
            id.toString(), 
            "Todas las sesiones del usuario fueron cerradas por un administrador",
            true
        );
        
        return ResponseEntity.ok(
            ApiResponse.<Void>builder()
                .success(true)
                .message("Todas las sesiones han sido cerradas")
                .build()
        );
    }
}