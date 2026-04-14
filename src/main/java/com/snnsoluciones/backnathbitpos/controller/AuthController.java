package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.auth.*;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.security.ContextoUsuario;
import com.snnsoluciones.backnathbitpos.service.auth.AuthService;
import com.snnsoluciones.backnathbitpos.service.auth.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints de autenticación")
public class AuthController {
    
    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @Operation(summary = "Solicitar reset de contraseña")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
        @Valid @RequestBody ForgotPasswordRequest request) {

        passwordResetService.solicitarReset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Código enviado al correo", null));
    }

    @Operation(summary = "Resetear contraseña con código")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
        @Valid @RequestBody ResetPasswordRequest request) {

        if (!request.getNuevaPassword().equals(request.getConfirmarPassword())) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Las contraseñas no coinciden"));
        }

        passwordResetService.resetPassword(
            request.getEmail(),
            request.getCodigo(),
            request.getNuevaPassword(),
            request.getConfirmarPassword()
        );
        return ResponseEntity.ok(ApiResponse.ok("Contraseña actualizada exitosamente", null));
    }
    
    @Operation(summary = "Login de usuario")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login exitoso", response));
    }
    
    @Operation(summary = "Establecer contexto de trabajo")
    @PostMapping("/contexto")
    public ResponseEntity<ApiResponse<TokenResponse>> establecerContexto(
            @Valid @RequestBody ContextoRequest request) {
        
        TokenResponse response = authService.establecerContexto(request);
        return ResponseEntity.ok(ApiResponse.ok("Contexto establecido", response));
    }
    
    @Operation(summary = "Refrescar token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @RequestParam String refreshToken) {
        
        TokenResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok(ApiResponse.ok("Token actualizado", response));
    }

    @Operation(summary = "Cambiar contraseña temporal")
    @PostMapping("/cambiar-password-temporal")
    public ResponseEntity<ApiResponse<TokenResponse>> cambiarPasswordTemporal(
        @Valid @RequestBody CambiarPasswordRequest request,
        Authentication authentication) {

        // Validar que las contraseñas coincidan
        if (!request.getNuevaPassword().equals(request.getConfirmarPassword())) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Las contraseñas no coinciden"));
        }

        ContextoUsuario contexto = (ContextoUsuario) authentication.getPrincipal();
        TokenResponse response = authService.cambiarPasswordTemporal(
            contexto.getEmail(),
            request.getNuevaPassword()
        );

        return ResponseEntity.ok(
            ApiResponse.ok("Contraseña actualizada exitosamente", response)
        );
    }
}