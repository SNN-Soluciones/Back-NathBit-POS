package com.snnsoluciones.backnathbitpos.controller.auth;

import com.snnsoluciones.backnathbitpos.config.security.JwtTokenProvider;
import com.snnsoluciones.backnathbitpos.dto.TenantSelectionRequest;
import com.snnsoluciones.backnathbitpos.dto.TenantSelectionResponse;
import com.snnsoluciones.backnathbitpos.dto.request.LoginRequest;
import com.snnsoluciones.backnathbitpos.dto.response.LoginResponse;
import com.snnsoluciones.backnathbitpos.entity.security.UsuarioTenant;
import com.snnsoluciones.backnathbitpos.exception.TenantException;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.repository.UsuarioTenantRepository;
import com.snnsoluciones.backnathbitpos.service.auth.AuthService;
import com.snnsoluciones.backnathbitpos.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticación", description = "Endpoints para autenticación y autorización")
public class AuthController {

  private final AuthService authService;
  private final UsuarioTenantRepository usuarioTenantRepository;
  private final JwtTokenProvider tokenProvider;

  @PostMapping("/login")
  @Operation(summary = "Login de usuario", description = "Autentica un usuario y devuelve los tokens de acceso")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
    log.info("Login request para: {}", loginRequest.getEmail());
    LoginResponse response = authService.login(loginRequest, ipAddress);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/refresh")
  @Operation(summary = "Refrescar token", description = "Genera un nuevo access token usando el refresh token")
  public ResponseEntity<LoginResponse> refreshToken(@RequestParam String refreshToken) {
    LoginResponse response = authService.refreshToken(refreshToken);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/logout")
  @Operation(summary = "Logout", description = "Cierra la sesión del usuario")
  public ResponseEntity<String> logout(@RequestHeader("Authorization") String token) {
    authService.logout(token);
    return ResponseEntity.ok("Logout exitoso");
  }

  // AuthController.java
  @PostMapping("/select-tenant")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<TenantSelectionResponse> selectTenant(@RequestBody TenantSelectionRequest request) {
    String userEmail = SecurityUtils.getCurrentUsername()
        .orElseThrow(() -> new UnauthorizedException("Usuario no autenticado"));

    // Validar que el usuario tiene acceso al tenant
    UsuarioTenant usuarioTenant = usuarioTenantRepository
        .findByUsuarioEmailAndTenantIdAndActivo(userEmail, request.getTenantId(), true)
        .orElseThrow(() -> new TenantException("No tiene acceso a este tenant"));

    // Actualizar fecha de último acceso
    usuarioTenant.setFechaAcceso(LocalDateTime.now());
    usuarioTenantRepository.save(usuarioTenant);

    // Generar nuevo token con tenant incluido (opcional)
    String enhancedToken = tokenProvider.generateTokenWithTenant(
        userEmail,
        request.getTenantId(),
        usuarioTenant.getRol().getNombre().name()
    );

    return ResponseEntity.ok(TenantSelectionResponse.builder()
        .accessToken(enhancedToken)
        .tenantId(request.getTenantId())
        .tenantNombre(usuarioTenant.getTenantNombre())
        .rol(usuarioTenant.getRol().getNombre().name())
        .build());
  }
}