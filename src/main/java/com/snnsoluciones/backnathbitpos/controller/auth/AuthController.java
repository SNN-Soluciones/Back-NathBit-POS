package com.snnsoluciones.backnathbitpos.controller.auth;

import com.snnsoluciones.backnathbitpos.config.security.JwtTokenProvider;
import com.snnsoluciones.backnathbitpos.dto.request.LoginRequest;
import com.snnsoluciones.backnathbitpos.dto.request.RefreshTokenRequest;
import com.snnsoluciones.backnathbitpos.dto.request.TenantSelectionRequest;
import com.snnsoluciones.backnathbitpos.dto.response.LoginResponse;
import com.snnsoluciones.backnathbitpos.dto.response.RefreshTokenResponse;
import com.snnsoluciones.backnathbitpos.dto.response.TenantSelectionResponse;
import com.snnsoluciones.backnathbitpos.entity.security.UsuarioTenant;
import com.snnsoluciones.backnathbitpos.exception.TenantException;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.repository.UsuarioTenantRepository;
import com.snnsoluciones.backnathbitpos.service.auth.AuthService;
import com.snnsoluciones.backnathbitpos.util.IpAddressUtils;
import com.snnsoluciones.backnathbitpos.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticación", description = "Endpoints para autenticación y autorización")
public class AuthController {

  private final AuthService authService;
  private final UsuarioTenantRepository usuarioTenantRepository;
  private final JwtTokenProvider tokenProvider;

  @PostMapping("/login")
  @Operation(summary = "Login de usuario", description = "Autentica un usuario y devuelve los tokens de acceso")
  public ResponseEntity<LoginResponse> login(
      @Valid @RequestBody LoginRequest loginRequest,
      HttpServletRequest request) {

    log.info("Login request para: {}", loginRequest.getEmail());

    // Obtener IP usando la utilidad
    String ipAddress = IpAddressUtils.getClientIpAddress(request);
    log.debug("IP Address del cliente: {}", ipAddress);

    LoginResponse response = authService.login(loginRequest, ipAddress);
    return ResponseEntity.ok(response);
  }

  // Alternativa: Si no quieres pasar HttpServletRequest como parámetro
  @PostMapping("/login-v2")
  @Operation(summary = "Login de usuario V2", description = "Autentica un usuario (obtiene IP automáticamente)")
  public ResponseEntity<LoginResponse> loginV2(@Valid @RequestBody LoginRequest loginRequest) {
    log.info("Login request para: {}", loginRequest.getEmail());

    // Obtener IP del contexto actual automáticamente
    String ipAddress = IpAddressUtils.getClientIpAddress();

    // Obtener información completa del cliente
    IpAddressUtils.ClientInfo clientInfo = IpAddressUtils.getClientInfo();
    log.debug("Cliente info - IP: {}, User-Agent: {}",
        clientInfo.getIpAddress(),
        clientInfo.getUserAgent());

    LoginResponse response = authService.login(loginRequest, ipAddress);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/refresh")
  @Operation(summary = "Refrescar token", description = "Genera un nuevo access token usando el refresh token")
  public ResponseEntity<RefreshTokenResponse> refreshToken(@RequestParam RefreshTokenRequest refreshToken) {
    RefreshTokenResponse response = authService.refreshToken(refreshToken);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/logout")
  @Operation(summary = "Logout", description = "Cierra la sesión del usuario")
  public ResponseEntity<String> logout(@RequestHeader("Authorization") String token) {
    authService.logout(token);
    return ResponseEntity.ok("Logout exitoso");
  }

  @PostMapping("/select-tenant")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Seleccionar tenant", description = "Permite al usuario seleccionar una empresa/tenant para trabajar")
  public ResponseEntity<TenantSelectionResponse> selectTenant(@Valid @RequestBody TenantSelectionRequest request) {
    String userEmail = SecurityUtils.getCurrentUsername()
        .orElseThrow(() -> new UnauthorizedException("Usuario no autenticado"));

    log.info("Usuario {} seleccionando tenant: {}", userEmail, request.getTenantId());

    // Validar que el usuario tiene acceso al tenant
    UsuarioTenant usuarioTenant = usuarioTenantRepository
        .findByUsuarioEmailAndTenantIdAndActivo(userEmail, request.getTenantId(), true)
        .orElseThrow(() -> new TenantException("No tiene acceso a este tenant"));

    // Actualizar fecha de último acceso
    usuarioTenant.setFechaAcceso(LocalDateTime.now());
    usuarioTenantRepository.save(usuarioTenant);

    // Generar nuevo token con tenant incluido
    String enhancedToken = tokenProvider.generateTokenWithTenant(
        userEmail,
        request.getTenantId(),
        usuarioTenant.getRol() != null ? usuarioTenant.getRol().getNombre().name() : "USER"
    );

    // Registrar el cambio de tenant en auditoría
    String currentIp = IpAddressUtils.getClientIpAddress();
    log.info("Usuario {} cambió al tenant {} desde IP {}", userEmail, request.getTenantId(), currentIp);

    return ResponseEntity.ok(TenantSelectionResponse.builder()
        .accessToken(enhancedToken)
        .tenantId(request.getTenantId())
        .tenantNombre(usuarioTenant.getTenantNombre())
        .rol(usuarioTenant.getRol() != null ? usuarioTenant.getRol().getNombre().name() : "USER")
        .build());
  }
}