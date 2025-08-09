package com.snnsoluciones.backnathbitpos.controller.auth;

import com.snnsoluciones.backnathbitpos.dto.auth.*;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para autenticación y autorización
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints para autenticación y gestión de sesiones")
public class AuthController {

    private final AuthService authService;

    @Operation(
        summary = "Login de usuario",
        description = "Autentica un usuario y retorna tokens JWT. La respuesta varía según el rol del usuario."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Login exitoso"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Credenciales inválidas"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "423",
            description = "Usuario bloqueado"
        )
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
        @Valid @RequestBody LoginRequest loginRequest) {

        log.info("Login request para: {}", loginRequest.getEmail());

        LoginResponse response = authService.login(loginRequest);

        return ResponseEntity.ok(
            ApiResponse.<LoginResponse>builder()
                .success(true)
                .message("Login exitoso")
                .data(response)
                .build()
        );
    }

    @Operation(
        summary = "Seleccionar contexto de trabajo",
        description = "Establece la empresa y sucursal de trabajo para usuarios con múltiples accesos"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Contexto establecido exitosamente"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "No autorizado"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "No tiene acceso a la empresa/sucursal seleccionada"
        )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/establecer-contexto")
    public ResponseEntity<ApiResponse<TokenResponse>> establecerContexto(
        @Valid @RequestBody SeleccionContextoRequest request) {

        log.info("Estableciendo contexto - Empresa: {}, Sucursal: {}",
            request.getEmpresaId(), request.getSucursalId());

        TokenResponse response = authService.seleccionarContexto(request);

        return ResponseEntity.ok(
            ApiResponse.<TokenResponse>builder()
                .success(true)
                .message("Contexto establecido exitosamente")
                .data(response)
                .build()
        );
    }

    @Operation(
        summary = "Renovar token",
        description = "Renueva el token de acceso usando el refresh token"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Token renovado exitosamente"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Refresh token inválido o expirado"
        )
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
        @Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {

        log.info("Renovando token");

        TokenResponse response = authService.refresh(refreshTokenRequest);

        return ResponseEntity.ok(
            ApiResponse.<TokenResponse>builder()
                .success(true)
                .message("Token renovado exitosamente")
                .data(response)
                .build()
        );
    }

    @Operation(
        summary = "Cerrar sesión",
        description = "Invalida el token actual y limpia el contexto del usuario"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {

        String token = extractTokenFromRequest(request);

        if (token != null) {
            authService.logout(token);
        }

        return ResponseEntity.ok(
            ApiResponse.<Void>builder()
                .success(true)
                .message("Sesión cerrada exitosamente")
                .build()
        );
    }

    @Operation(
        summary = "Obtener contexto actual",
        description = "Obtiene el contexto de trabajo actual del usuario autenticado"
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/contexto")
    public ResponseEntity<ApiResponse<ContextoDTO>> obtenerContexto(HttpServletRequest request) {

        String token = extractTokenFromRequest(request);
        ContextoDTO contexto = authService.obtenerContextoDesdeToken(token);

        if (contexto == null) {
            return ResponseEntity.ok(
                ApiResponse.<ContextoDTO>builder()
                    .success(true)
                    .message("No hay contexto establecido")
                    .data(null)
                    .build()
            );
        }

        return ResponseEntity.ok(
            ApiResponse.<ContextoDTO>builder()
                .success(true)
                .message("Contexto obtenido exitosamente")
                .data(contexto)
                .build()
        );
    }

    @Operation(
        summary = "Validar token",
        description = "Verifica si un token es válido"
    )
    @Parameter(name = "token", description = "Token JWT a validar", required = true)
    @GetMapping("/validar")
    public ResponseEntity<ApiResponse<Boolean>> validarToken(
        @RequestParam String token) {

        boolean esValido = authService.validarToken(token);

        return ResponseEntity.ok(
            ApiResponse.<Boolean>builder()
                .success(true)
                .message(esValido ? "Token válido" : "Token inválido")
                .data(esValido)
                .build()
        );
    }

    @Operation(
        summary = "Cambiar contraseña",
        description = "Permite al usuario cambiar su contraseña actual"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/cambiar-password")
    public ResponseEntity<ApiResponse<Void>> cambiarPassword(
        @Valid @RequestBody CambiarPasswordRequest request) {

        log.info("Cambio de contraseña solicitado");

        // TODO: Implementar en AuthService

        return ResponseEntity.ok(
            ApiResponse.<Void>builder()
                .success(true)
                .message("Contraseña actualizada exitosamente")
                .build()
        );
    }

    @Operation(
        summary = "Solicitar recuperación de contraseña",
        description = "Envía un email con instrucciones para recuperar la contraseña"
    )
    @PostMapping("/recuperar-password")
    public ResponseEntity<ApiResponse<Void>> recuperarPassword(
        @Valid @RequestBody RecuperarPasswordRequest request) {

        log.info("Recuperación de contraseña solicitada para: {}", request.getEmail());

        // TODO: Implementar en AuthService

        return ResponseEntity.accepted()
            .body(ApiResponse.<Void>builder()
                .success(true)
                .message("Se han enviado las instrucciones a tu correo")
                .build()
            );
    }

    @Operation(
        summary = "Restablecer contraseña",
        description = "Restablece la contraseña usando el token de recuperación"
    )
    @PostMapping("/restablecer-password")
    public ResponseEntity<ApiResponse<Void>> restablecerPassword(
        @Valid @RequestBody RestablecerPasswordRequest request) {

        log.info("Restablecimiento de contraseña con token");

        // TODO: Implementar en AuthService

        return ResponseEntity.ok(
            ApiResponse.<Void>builder()
                .success(true)
                .message("Contraseña restablecida exitosamente")
                .build()
        );
    }

    /**
     * Extrae el token JWT del header Authorization
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}