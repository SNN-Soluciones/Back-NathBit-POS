package com.snnsoluciones.backnathbitpos.controller.auth;

import com.snnsoluciones.backnathbitpos.dto.auth.SeleccionContextoRequest;
import com.snnsoluciones.backnathbitpos.dto.auth.SeleccionContextoResponse;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.request.ChangePasswordRequest;
import com.snnsoluciones.backnathbitpos.dto.request.ForgotPasswordRequest;
import com.snnsoluciones.backnathbitpos.dto.request.LoginRequest;
import com.snnsoluciones.backnathbitpos.dto.request.RefreshTokenRequest;
import com.snnsoluciones.backnathbitpos.dto.response.ContextoActual;
import com.snnsoluciones.backnathbitpos.dto.response.LoginResponse;
import com.snnsoluciones.backnathbitpos.dto.response.RefreshTokenResponse;
import com.snnsoluciones.backnathbitpos.dto.response.TokenValidationResponse;
import com.snnsoluciones.backnathbitpos.service.auth.AuthServiceV2;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticación", description = "Endpoints para autenticación y autorización")
public class AuthController {
  private final AuthServiceV2 authService;

  /**
   * Login principal - no requiere tenant
   */
  @PostMapping("/login")
  @Operation(summary = "Login de usuario",
      description = "Autentica al usuario y determina el flujo según su tipo (operativo/administrativo)")
  public ResponseEntity<ApiResponse<LoginResponse>> login(
      @Valid @RequestBody LoginRequest loginRequest,
      HttpServletRequest request) {

    String ipAddress = getClientIpAddress(request);
    String userAgent = request.getHeader("User-Agent");

    log.info("Intento de login para usuario: {} desde IP: {}", loginRequest.getEmail(), ipAddress);

    LoginResponse response = authService.login(loginRequest, ipAddress, userAgent);

    return ResponseEntity.ok(
        ApiResponse.<LoginResponse>builder()
            .success(true)
            .message("Login exitoso")
            .data(response)
            .build()
    );
  }

  /**
   * Selección de contexto (empresa/sucursal) después del login inicial
   */
  @PostMapping("/select-context")
  @Operation(summary = "Seleccionar contexto",
      description = "Selecciona la empresa y sucursal después del login inicial")
  public ResponseEntity<ApiResponse<SeleccionContextoResponse>> seleccionarContexto(
      @Valid @RequestBody SeleccionContextoRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {

    log.info("Usuario {} seleccionando contexto - Empresa: {}, Sucursal: {}",
        userDetails.getUsername(), request.getEmpresaId(), request.getSucursalId());

    SeleccionContextoResponse response = authService.seleccionarContexto(
        request, userDetails.getUsername()
    );

    return ResponseEntity.ok(
        ApiResponse.<SeleccionContextoResponse>builder()
            .success(true)
            .message("Contexto seleccionado exitosamente")
            .data(response)
            .build()
    );
  }

  /**
   * Refresh token
   */
  @PostMapping("/refresh")
  @Operation(summary = "Refrescar token",
      description = "Genera un nuevo access token usando el refresh token")
  public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
      @Valid @RequestBody RefreshTokenRequest request) {

    log.debug("Solicitud de refresh token");

    // TODO: Implementar en AuthServiceV2
    RefreshTokenResponse response = RefreshTokenResponse.builder()
        .accessToken("new-access-token")
        .tokenType("Bearer")
        .expiresIn(3600L)
        .build();

    return ResponseEntity.ok(
        ApiResponse.<RefreshTokenResponse>builder()
            .success(true)
            .message("Token actualizado")
            .data(response)
            .build()
    );
  }

  /**
   * Logout
   */
  @PostMapping("/logout")
  @Operation(summary = "Cerrar sesión", description = "Invalida el token actual")
  public ResponseEntity<ApiResponse<Void>> logout(
      @RequestHeader("Authorization") String authHeader,
      @AuthenticationPrincipal UserDetails userDetails) {

    log.info("Usuario {} cerrando sesión", userDetails.getUsername());

    String token = authHeader.replace("Bearer ", "");
    // TODO: Implementar invalidación de token

    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .success(true)
            .message("Sesión cerrada exitosamente")
            .build()
    );
  }

  /**
   * Cambiar contraseña
   */
  @PostMapping("/change-password")
  @Operation(summary = "Cambiar contraseña",
      description = "Permite al usuario cambiar su contraseña")
  public ResponseEntity<ApiResponse<Void>> changePassword(
      @Valid @RequestBody ChangePasswordRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {

    log.info("Usuario {} cambiando contraseña", userDetails.getUsername());

    // TODO: Implementar cambio de contraseña

    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .success(true)
            .message("Contraseña actualizada exitosamente")
            .build()
    );
  }

  /**
   * Solicitar recuperación de contraseña
   */
  @PostMapping("/forgot-password")
  @Operation(summary = "Recuperar contraseña",
      description = "Inicia el proceso de recuperación de contraseña")
  public ResponseEntity<ApiResponse<Void>> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request) {

    log.info("Solicitud de recuperación de contraseña para: {}", request.getEmail());

    // TODO: Implementar recuperación de contraseña

    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .success(true)
            .message("Si el email existe, recibirá instrucciones para recuperar su contraseña")
            .build()
    );
  }

  /**
   * Validar token
   */
  @GetMapping("/validate")
  @Operation(summary = "Validar token",
      description = "Verifica si el token actual es válido")
  public ResponseEntity<ApiResponse<TokenValidationResponse>> validateToken(
      @AuthenticationPrincipal UserDetails userDetails) {

    // Si llegamos aquí, el token es válido (Spring Security lo validó)
    TokenValidationResponse response = TokenValidationResponse.builder()
        .valid(true)
        .username(userDetails.getUsername())
        .authorities(userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList())
        .build();

    return ResponseEntity.ok(
        ApiResponse.<TokenValidationResponse>builder()
            .success(true)
            .message("Token válido")
            .data(response)
            .build()
    );
  }

  /**
   * Obtener información del contexto actual
   */
  @GetMapping("/current-context")
  @Operation(summary = "Contexto actual",
      description = "Obtiene información del contexto actual del usuario")
  public ResponseEntity<ApiResponse<ContextoActual>> getCurrentContext(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestAttribute(value = "empresaId", required = false) UUID empresaId,
      @RequestAttribute(value = "sucursalId", required = false) UUID sucursalId,
      @RequestAttribute(value = "tenantId", required = false) String tenantId) {

    ContextoActual contexto = ContextoActual.builder()
        .username(userDetails.getUsername())
        .empresaId(empresaId)
        .sucursalId(sucursalId)
        .tenantId(tenantId)
        .authorities(userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList())
        .build();

    return ResponseEntity.ok(
        ApiResponse.<ContextoActual>builder()
            .success(true)
            .message("Contexto actual del usuario")
            .data(contexto)
            .build()
    );
  }

  // Métodos auxiliares
  private String getClientIpAddress(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }

    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp;
    }

    return request.getRemoteAddr();
  }
}