package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.auth.LoginRequest;
import com.snnsoluciones.backnathbitpos.dto.auth.LoginResponse;
import com.snnsoluciones.backnathbitpos.dto.auth.RefreshTokenRequest;
import com.snnsoluciones.backnathbitpos.dto.auth.SeleccionContextoRequest;
import com.snnsoluciones.backnathbitpos.dto.auth.TokenResponse;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.usuario.CrearUsuarioRequest;
import com.snnsoluciones.backnathbitpos.dto.usuario.UsuarioDTO;
import com.snnsoluciones.backnathbitpos.service.auth.AuthService;
import com.snnsoluciones.backnathbitpos.service.usuario.UsuarioGestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para autenticación y registro de usuarios.
 * Maneja el login, registro, refresh de tokens y selección de contexto.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    private final UsuarioGestionService usuarioGestionService;
    
    /**
     * Endpoint para login de usuarios.
     * 
     * @param loginRequest credenciales de usuario
     * @return ApiResponse con LoginResponse que incluye token y datos del usuario
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest) {
        log.info("Intento de login para email: {}", loginRequest.getEmail());
        
        try {
            LoginResponse response = authService.login(loginRequest);
            return ResponseEntity.ok(
                ApiResponse.success("Login exitoso", response)
            );
        } catch (Exception e) {
            log.error("Error en login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Endpoint para registro de nuevos usuarios.
     * Público para permitir el auto-registro inicial.
     * 
     * @param request datos del nuevo usuario
     * @return ApiResponse con UsuarioDTO creado
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UsuarioDTO>> register(
            @Valid @RequestBody CrearUsuarioRequest request) {
        log.info("Registro de nuevo usuario: {}", request.getEmail());
        
        try {
            UsuarioDTO usuario = usuarioGestionService.crearUsuario(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Usuario registrado exitosamente", usuario));
        } catch (Exception e) {
            log.error("Error en registro: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Endpoint para seleccionar contexto (empresa/sucursal).
     * Solo para usuarios autenticados con múltiples accesos.
     * 
     * @param request contiene empresaId y opcionalmente sucursalId
     * @return ApiResponse con nuevo token contextualizado
     */
    @PostMapping("/seleccionar-contexto")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TokenResponse>> seleccionarContexto(
            @Valid @RequestBody SeleccionContextoRequest request) {
        log.info("Selección de contexto - Empresa: {}, Sucursal: {}", 
                request.getEmpresaId(), request.getSucursalId());
        
        try {
            TokenResponse response = authService.seleccionarContexto(request);
            return ResponseEntity.ok(
                ApiResponse.success("Contexto seleccionado", response)
            );
        } catch (Exception e) {
            log.error("Error al seleccionar contexto: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Endpoint para renovar token de acceso.
     * 
     * @param request contiene el refresh token
     * @return ApiResponse con nuevos tokens
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.debug("Solicitud de refresh token");
        
        try {
            TokenResponse response = authService.refresh(request);
            return ResponseEntity.ok(
                ApiResponse.success("Token renovado", response)
            );
        } catch (Exception e) {
            log.error("Error al renovar token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Endpoint para cerrar sesión.
     * Invalida el token actual.
     * 
     * @param bearerToken token en el header Authorization
     * @return ApiResponse confirmando logout
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String bearerToken) {
        log.info("Solicitud de logout");
        
        try {
            String token = bearerToken.substring(7); // Remover "Bearer "
            authService.logout(token);
            return ResponseEntity.ok(
                ApiResponse.success("Sesión cerrada exitosamente", null)
            );
        } catch (Exception e) {
            log.error("Error en logout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
}