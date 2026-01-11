package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.auth.CambiarPinRequest;
import com.snnsoluciones.backnathbitpos.dto.auth.LoginPdvRequest;
import com.snnsoluciones.backnathbitpos.dto.auth.LoginPdvResponse;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.security.ContextoUsuario;
import com.snnsoluciones.backnathbitpos.service.AuthPdvService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para autenticación con PIN en dispositivos PDV
 * 
 * Endpoints con device token (header X-Device-Token):
 * - POST /api/auth/login-pdv - Login con PIN
 * 
 * Endpoints autenticados (JWT):
 * - POST /api/auth/cambiar-pin - Cambiar PIN del usuario
 * - POST /api/auth/generar-pin - Generar PIN aleatorio (Admin)
 * - POST /api/auth/resetear-pin - Resetear PIN (Admin)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticación PDV", description = "Login y gestión de PIN para dispositivos PDV")
public class AuthPdvController {
    
    private final AuthPdvService authPdvService;
    
    private static final String HEADER_DEVICE_TOKEN = "X-Device-Token";
    
    // ==================== LOGIN CON PIN ====================
    
    @Operation(summary = "Login con PIN en PDV", 
               description = "Autenticación de usuario con PIN de 4-6 dígitos en dispositivo PDV")
    @PostMapping("/login-pdv")
    public ResponseEntity<ApiResponse<LoginPdvResponse>> loginPdv(
            @Parameter(description = "Token del dispositivo", required = true)
            @RequestHeader(HEADER_DEVICE_TOKEN) String deviceToken,
            @Valid @RequestBody LoginPdvRequest request) {
        
        log.info("POST /api/auth/login-pdv - Usuario: {}", request.getUsuarioId());
        
        try {
            LoginPdvResponse response = authPdvService.loginConPin(deviceToken, request);
            
            String mensaje = response.getRequiereCambioPin() 
                ? "Login exitoso - Cambio de PIN requerido" 
                : "Login exitoso";
            
            return ResponseEntity.ok(ApiResponse.success(mensaje, response));
        } catch (Exception e) {
            log.error("Error en login PDV: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // ==================== GESTIÓN DE PIN ====================
    
    @Operation(summary = "Cambiar PIN", 
               description = "Permite al usuario cambiar su PIN")
    @PostMapping("/cambiar-pin")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> cambiarPin(
            @Valid @RequestBody CambiarPinRequest request,
            Authentication authentication) {
        
        log.info("POST /api/auth/cambiar-pin");
        
        try {
            Long usuarioId = obtenerUsuarioIdDeAuth(authentication);
            authPdvService.cambiarPin(usuarioId, request);
            
            return ResponseEntity.ok(ApiResponse.success("PIN cambiado exitosamente", null));
        } catch (Exception e) {
            log.error("Error cambiando PIN: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @Operation(summary = "Generar PIN aleatorio", 
               description = "Genera un PIN aleatorio de 4 dígitos para un usuario (Solo Admin)")
    @PostMapping("/generar-pin/{usuarioId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> generarPin(@PathVariable Long usuarioId) {
        
        log.info("POST /api/auth/generar-pin/{}", usuarioId);
        
        try {
            String pin = authPdvService.generarPinAleatorio(usuarioId);
            
            return ResponseEntity.ok(ApiResponse.success(
                "PIN generado exitosamente. El usuario debe cambiarlo en el primer login.",
                pin
            ));
        } catch (Exception e) {
            log.error("Error generando PIN: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @Operation(summary = "Resetear PIN", 
               description = "Resetea el PIN de un usuario generando uno nuevo aleatorio (Solo Admin)")
    @PostMapping("/resetear-pin/{usuarioId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> resetearPin(@PathVariable Long usuarioId) {
        
        log.info("POST /api/auth/resetear-pin/{}", usuarioId);
        
        try {
            String pin = authPdvService.resetearPin(usuarioId);
            
            return ResponseEntity.ok(ApiResponse.success(
                "PIN reseteado exitosamente. El usuario debe cambiarlo en el primer login.",
                pin
            ));
        } catch (Exception e) {
            log.error("Error reseteando PIN: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // ==================== HELPERS ====================
    
    /**
     * Extrae el ID del usuario del Authentication
     */
    private Long obtenerUsuarioIdDeAuth(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Usuario no autenticado");
        }
        
        Object principal = authentication.getPrincipal();
        
        // Si es ContextoUsuario (tu implementación actual)
        if (principal instanceof ContextoUsuario) {
            return ((ContextoUsuario) principal).getUserId();
        }
        
        throw new RuntimeException("No se pudo obtener el ID del usuario");
    }
}