package com.snnsoluciones.backnathbitpos.service.auth.impl;

import com.snnsoluciones.backnathbitpos.config.security.JwtTokenProvider;
import com.snnsoluciones.backnathbitpos.dto.request.LoginRequest;
import com.snnsoluciones.backnathbitpos.dto.request.RefreshTokenRequest;
import com.snnsoluciones.backnathbitpos.dto.request.TenantSelectionRequest;
import com.snnsoluciones.backnathbitpos.dto.response.RefreshTokenResponse;
import com.snnsoluciones.backnathbitpos.dto.response.TenantInfo;
import com.snnsoluciones.backnathbitpos.dto.response.TenantSelectionResponse;
import com.snnsoluciones.backnathbitpos.dto.response.LoginResponse;
import com.snnsoluciones.backnathbitpos.entity.security.AuditEvent;
import com.snnsoluciones.backnathbitpos.entity.security.Usuario;
import com.snnsoluciones.backnathbitpos.entity.security.UsuarioTenant;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.repository.AuditEventRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioTenantRepository;
import com.snnsoluciones.backnathbitpos.service.auth.AuthService;
import com.snnsoluciones.backnathbitpos.service.auth.RateLimiterService;
import com.snnsoluciones.backnathbitpos.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

  private final UsuarioRepository usuarioRepository;
  private final UsuarioTenantRepository usuarioTenantRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider tokenProvider;
  private final RateLimiterService rateLimiter;
  private final AuditEventRepository auditEventRepository;

  @Value("${app.jwt.expiration:3600}")
  private Long jwtExpiration;

  @Value("${app.security.max-intentos-login:3}")
  private int maxIntentosLogin;

  @Override
  public LoginResponse login(LoginRequest loginRequest, String ipAddress) {
    String rateLimitKey = ipAddress + ":" + loginRequest.getEmail();

    // Verificar rate limiting
    if (rateLimiter.isBlocked(rateLimitKey)) {
      long minutesRemaining = rateLimiter.getBlockedMinutesRemaining(rateLimitKey);
      String message = String.format(
          "Demasiados intentos fallidos. Por favor intente nuevamente en %d minutos.",
          minutesRemaining
      );
      log.warn("Login bloqueado para {} desde IP {}", loginRequest.getEmail(), ipAddress);
      throw new BusinessException(message);
    }

    try {
      // Buscar usuario por email (sin tenant)
      Usuario usuario = usuarioRepository.findByEmail(loginRequest.getEmail())
          .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));

      // Verificar contraseña
      if (!passwordEncoder.matches(loginRequest.getPassword(), usuario.getPassword())) {
        throw new BadCredentialsException("Credenciales incorrectas");
      }

      // Verificar si el usuario está activo
      if (!usuario.getActivo()) {
        throw new BusinessException("Usuario inactivo");
      }

      // Verificar si está bloqueado
      if (usuario.getBloqueado()) {
        throw new BusinessException("Usuario bloqueado");
      }

      // Login exitoso - resetear contador de intentos
      rateLimiter.loginSucceeded(rateLimitKey);
      usuario.setIntentosFallidos(0);
      usuario.setUltimoAcceso(LocalDateTime.now());
      usuarioRepository.save(usuario);

      // Generar tokens sin tenant específico
      String accessToken = tokenProvider.generateTokenFromUsername(usuario.getUsername());
      String refreshToken = tokenProvider.generateRefreshToken(usuario.getEmail());

      // Obtener lista de tenants disponibles para el usuario
      List<TenantInfo> tenants = usuarioTenantRepository
          .findByUsuarioIdAndActivo(usuario.getId(), true)
          .stream()
          .map(ut -> TenantInfo.builder()
              .tenantId(ut.getTenantId())
              .tenantNombre(ut.getTenantNombre())
              .tenantTipo(ut.getTenantTipo())
              .rol(ut.getRol() != null ? ut.getRol().getNombre().name() : "USER")
              .esPropietario(ut.isEsPropietario())
              .build())
          .collect(Collectors.toList());

      // Registrar evento de login exitoso
      registrarEventoLogin(usuario.getEmail(), ipAddress, true, "Login exitoso - Multi-tenant");

      // Construir respuesta con lista de tenants
      return LoginResponse.builder()
          .accessToken(accessToken)
          .refreshToken(refreshToken)
          .tokenType("Bearer")
          .expiresIn(jwtExpiration)
          .email(usuario.getEmail())
          .nombre(usuario.getNombre())
          .apellidos(usuario.getApellidos())
          .tenants(tenants) // Lista de empresas disponibles
          .requiresTenantSelection(!tenants.isEmpty()) // Indica si debe seleccionar tenant
          .build();

    } catch (BadCredentialsException e) {
      // Registrar intento fallido
      rateLimiter.loginFailed(rateLimitKey, getCurrentRequest());
      registrarIntentoFallido(loginRequest.getEmail());

      int remainingAttempts = rateLimiter.getRemainingAttempts(rateLimitKey);
      String message = remainingAttempts > 0
          ? String.format("Credenciales incorrectas. Intentos restantes: %d", remainingAttempts)
          : "Credenciales incorrectas. Cuenta bloqueada temporalmente.";

      registrarEventoLogin(loginRequest.getEmail(), ipAddress, false, message);
      throw new UnauthorizedException(message);
    }
  }

  @Override
  public TenantSelectionResponse selectTenant(TenantSelectionRequest request) {
    String userEmail = SecurityUtils.getCurrentUsername()
        .orElseThrow(() -> new UnauthorizedException("Usuario no autenticado"));

    // Buscar usuario
    Usuario usuario = usuarioRepository.findByEmail(userEmail)
        .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

    // Validar que el usuario tiene acceso al tenant
    UsuarioTenant usuarioTenant = usuarioTenantRepository
        .findByUsuarioIdAndTenantIdAndActivo(usuario.getId(), request.getTenantId(), true)
        .orElseThrow(() -> new BusinessException("No tiene acceso a esta empresa"));

    // Actualizar fecha de último acceso al tenant
    usuarioTenant.setFechaAcceso(LocalDateTime.now());
    usuarioTenantRepository.save(usuarioTenant);

    // Generar nuevo token con información del tenant
    String enhancedToken = tokenProvider.generateTokenWithTenant(
        userEmail,
        request.getTenantId(),
        usuarioTenant.getRol() != null ? usuarioTenant.getRol().getNombre().name() : "USER"
    );

    // Registrar evento
    registrarEventoSeleccionTenant(userEmail, request.getTenantId(), usuarioTenant.getTenantNombre());

    return TenantSelectionResponse.builder()
        .accessToken(enhancedToken)
        .tenantId(request.getTenantId())
        .tenantNombre(usuarioTenant.getTenantNombre())
        .tenantTipo(usuarioTenant.getTenantTipo())
        .rol(usuarioTenant.getRol() != null ? usuarioTenant.getRol().getNombre().name() : "USER")
        .esPropietario(usuarioTenant.isEsPropietario())
        .build();
  }

  @Override
  public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
    if (!tokenProvider.isRefreshToken(request.getRefreshToken())) {
      throw new UnauthorizedException("Refresh token inválido");
    }

    String username = tokenProvider.getUsernameFromRefreshToken(request.getRefreshToken());
    String tenantId = tokenProvider.getTenantFromRefreshToken(request.getRefreshToken());

    // Generar nuevo access token
    String newAccessToken;
    if (tenantId != null) {
      // Si ya tenía tenant seleccionado, mantenerlo
      String role = tokenProvider.getRoleFromRefreshToken(request.getRefreshToken());
      newAccessToken = tokenProvider.generateTokenWithTenant(username, tenantId, role);
    } else {
      newAccessToken = tokenProvider.generateTokenFromUsername(username);
    }

    return RefreshTokenResponse.builder()
        .accessToken(newAccessToken)
        .tokenType("Bearer")
        .expiresIn(jwtExpiration)
        .build();
  }

  @Override
  public void logout(String token) {
    // Invalidar el token
    tokenProvider.invalidateToken(token);

    // Limpiar el contexto de seguridad
    SecurityContextHolder.clearContext();

    // Registrar evento
    String username = tokenProvider.getUsernameFromToken(token);
    registrarEventoLogout(username);
  }

  private void registrarIntentoFallido(String email) {
    usuarioRepository.findByEmail(email).ifPresent(usuario -> {
      usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);

      if (usuario.getIntentosFallidos() >= maxIntentosLogin) {
        usuario.setBloqueado(true);
        log.warn("Usuario {} bloqueado por exceder intentos de login", email);
      }

      usuarioRepository.save(usuario);
    });
  }

  private void registrarEventoLogin(String email, String ipAddress, boolean exitoso, String detalles) {
    AuditEvent event = new AuditEvent();
    event.setUsername(email);
    event.setEventType(exitoso ? "LOGIN_SUCCESS" : "LOGIN_FAILED");
    event.setEventDate(LocalDateTime.now());
    event.setIpAddress(ipAddress);
    event.setUserAgent(getUserAgent());
    event.setDetails(detalles);
    event.setSuccess(exitoso);

    auditEventRepository.save(event);
  }

  private void registrarEventoSeleccionTenant(String email, String tenantId, String tenantNombre) {
    AuditEvent event = new AuditEvent();
    event.setUsername(email);
    event.setEventType("TENANT_SELECTED");
    event.setEventDate(LocalDateTime.now());
    event.setIpAddress(getClientIpAddress());
    event.setUserAgent(getUserAgent());
    event.setDetails(String.format("Tenant seleccionado: %s (%s)", tenantNombre, tenantId));
    event.setSuccess(true);

    auditEventRepository.save(event);
  }

  private void registrarEventoLogout(String email) {
    AuditEvent event = new AuditEvent();
    event.setUsername(email);
    event.setEventType("LOGOUT");
    event.setEventDate(LocalDateTime.now());
    event.setIpAddress(getClientIpAddress());
    event.setUserAgent(getUserAgent());
    event.setDetails("Logout exitoso");
    event.setSuccess(true);

    auditEventRepository.save(event);
  }

  private HttpServletRequest getCurrentRequest() {
    ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    return attrs != null ? attrs.getRequest() : null;
  }

  private String getUserAgent() {
    HttpServletRequest request = getCurrentRequest();
    return request != null ? request.getHeader("User-Agent") : "Unknown";
  }

  private String getClientIpAddress() {
    HttpServletRequest request = getCurrentRequest();
    if (request == null) return "Unknown";

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