package com.snnsoluciones.backnathbitpos.service.auth.impl;

import com.snnsoluciones.backnathbitpos.config.security.JwtTokenProvider;
import com.snnsoluciones.backnathbitpos.config.security.RateLimiterConfig;
import com.snnsoluciones.backnathbitpos.dto.request.LoginRequest;
import com.snnsoluciones.backnathbitpos.dto.response.LoginResponse;
import com.snnsoluciones.backnathbitpos.entity.security.AuditEvent;
import com.snnsoluciones.backnathbitpos.entity.security.TokenBlacklist;
import com.snnsoluciones.backnathbitpos.entity.security.Usuario;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.AuditEventRepository;
import com.snnsoluciones.backnathbitpos.repository.TokenBlacklistRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.auth.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider tokenProvider;
  private final UsuarioRepository usuarioRepository;
  private final TokenBlacklistRepository tokenBlacklistRepository;
  private final AuditEventRepository auditEventRepository;
  private final CacheManager cacheManager;
  private final RateLimiterConfig rateLimiter;

  @Value("${spring.security.jwt.expiration}")
  private Long jwtExpiration;

  @Override
  @Transactional
  public LoginResponse login(LoginRequest loginRequest) {
    log.info("Intento de login para: {}", loginRequest.getEmail());

    String ipAddress = getClientIpAddress();
    String rateLimitKey = rateLimiter.generateKey(loginRequest.getEmail(), ipAddress);

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
      // Autenticar usuario
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(
              loginRequest.getEmail(),
              loginRequest.getPassword()
          )
      );

      SecurityContextHolder.getContext().setAuthentication(authentication);

      // Login exitoso - resetear contador de intentos
      rateLimiter.loginSucceeded(rateLimitKey);

      // Generar tokens
      String accessToken = tokenProvider.generateToken(authentication);
      String refreshToken = tokenProvider.generateRefreshToken(loginRequest.getEmail());

      // Obtener usuario y actualizar último acceso
      Usuario usuario = (Usuario) authentication.getPrincipal();
      usuario.setUltimoAcceso(LocalDateTime.now());
      usuario.setIntentosFallidos(0);
      usuarioRepository.save(usuario);

      // Registrar evento de login exitoso
      AuditEvent loginEvent = new AuditEvent();
      loginEvent.setUsername(usuario.getEmail());
      loginEvent.setEventType("LOGIN_SUCCESS");
      loginEvent.setEventDate(LocalDateTime.now());
      loginEvent.setIpAddress(ipAddress);
      loginEvent.setUserAgent(getUserAgent());
      loginEvent.setDetails("Login exitoso");
      loginEvent.setSuccess(true);

      auditEventRepository.save(loginEvent);

      // Construir respuesta
      return LoginResponse.builder()
          .accessToken(accessToken)
          .refreshToken(refreshToken)
          .tokenType("Bearer")
          .expiresIn(jwtExpiration)
          .email(usuario.getEmail())
          .nombre(usuario.getNombre())
          .apellidos(usuario.getApellidos())
          .roles(usuario.getRol() != null ? usuario.getRol().getNombre().name() : "USER")
          .tenantId(usuario.getTenantId())
          .build();

    } catch (BadCredentialsException e) {
      // Registrar intento fallido
      rateLimiter.loginFailed(rateLimitKey, getCurrentRequest());
      registrarIntentoFallido(loginRequest.getEmail());

      int remainingAttempts = rateLimiter.getRemainingAttempts(rateLimitKey);
      String message = remainingAttempts > 0
          ? String.format("Credenciales incorrectas. Intentos restantes: %d", remainingAttempts)
          : "Credenciales incorrectas. Cuenta bloqueada temporalmente.";

      throw new BusinessException(message);

    } catch (AuthenticationException e) {
      // Otros errores de autenticación
      rateLimiter.loginFailed(rateLimitKey, getCurrentRequest());
      registrarIntentoFallido(loginRequest.getEmail());

      log.error("Error de autenticación para {}: {}", loginRequest.getEmail(), e.getMessage());
      throw new BusinessException("Error de autenticación: " + e.getMessage());

    } catch (Exception e) {
      log.error("Error inesperado en login para {}: {}", loginRequest.getEmail(), e.getMessage());
      throw new BusinessException("Error al procesar login");
    }
  }

  @Override
  @Transactional
  public LoginResponse refreshToken(String refreshToken) {
    log.debug("Procesando refresh token");

    try {
      // Validar el refresh token
      if (!tokenProvider.validateToken(refreshToken)) {
        throw new BusinessException("Refresh token inválido");
      }

      // Verificar que no esté en blacklist
      if (tokenBlacklistRepository.existsByToken(refreshToken)) {
        throw new BusinessException("Refresh token ha sido revocado");
      }

      String username = tokenProvider.getUsernameFromToken(refreshToken);
      String newAccessToken = tokenProvider.generateTokenFromUsername(username);

      Usuario usuario = usuarioRepository.findByEmail(username)
          .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

      // Actualizar último acceso
      usuario.setUltimoAcceso(LocalDateTime.now());
      usuarioRepository.save(usuario);

      return LoginResponse.builder()
          .accessToken(newAccessToken)
          .refreshToken(refreshToken)
          .tokenType("Bearer")
          .expiresIn(jwtExpiration)
          .email(usuario.getEmail())
          .nombre(usuario.getNombre())
          .apellidos(usuario.getApellidos())
          .roles(usuario.getRol() != null ? usuario.getRol().getNombre().name() : "USER")
          .tenantId(usuario.getTenantId())
          .build();

    } catch (Exception e) {
      log.error("Error procesando refresh token: {}", e.getMessage());
      throw new BusinessException("Error al procesar refresh token");
    }
  }

  @Override
  @Transactional
  public void logout(String token) {
    try {
      // 1. Validar que el token no sea nulo o vacío
      if (token == null || token.trim().isEmpty()) {
        throw new BusinessException("Token inválido para logout");
      }

      // 2. Remover el prefijo "Bearer " si existe
      String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;

      // 3. Validar y extraer información del token
      Claims claims = tokenProvider.validateAndGetClaims(cleanToken);
      String username = claims.getSubject();
      Date expiration = claims.getExpiration();

      // 4. Agregar el token a la blacklist
      TokenBlacklist blacklistedToken = new TokenBlacklist();
      blacklistedToken.setToken(cleanToken);
      blacklistedToken.setUsername(username);
      blacklistedToken.setExpirationDate(expiration.toInstant()
          .atZone(ZoneId.systemDefault())
          .toLocalDateTime());
      blacklistedToken.setBlacklistedAt(LocalDateTime.now());
      blacklistedToken.setReason("LOGOUT");

      tokenBlacklistRepository.save(blacklistedToken);

      // 5. Actualizar último logout del usuario
      Usuario usuario = usuarioRepository.findByEmail(username)
          .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

      usuario.setUltimoAcceso(LocalDateTime.now());
      usuarioRepository.save(usuario);

      // 6. Si usas Redis/Cache, invalidar el token ahí también
      if (cacheManager != null) {
        Cache tokenCache = cacheManager.getCache("tokens");
        if (tokenCache != null) {
          tokenCache.evict(cleanToken);
        }

        // También limpiar cache de usuario
        Cache userCache = cacheManager.getCache("users");
        if (userCache != null) {
          userCache.evict(username);
        }
      }

      // 7. Registrar evento de logout para auditoría
      AuditEvent logoutEvent = new AuditEvent();
      logoutEvent.setUsername(username);
      logoutEvent.setEventType("LOGOUT");
      logoutEvent.setEventDate(LocalDateTime.now());
      logoutEvent.setIpAddress(getClientIpAddress());
      logoutEvent.setUserAgent(getUserAgent());
      logoutEvent.setDetails("Usuario cerró sesión exitosamente");
      logoutEvent.setSuccess(true);

      auditEventRepository.save(logoutEvent);

      // 8. Limpiar el contexto de seguridad
      SecurityContextHolder.clearContext();

      log.info("Usuario {} deslogueado exitosamente", username);

    } catch (ExpiredJwtException e) {
      // Si el token ya expiró, igual lo agregamos a la blacklist por seguridad
      log.warn("Intento de logout con token expirado: {}", e.getMessage());

      String username = e.getClaims().getSubject();
      TokenBlacklist blacklistedToken = new TokenBlacklist();
      blacklistedToken.setToken(token);
      blacklistedToken.setUsername(username);
      blacklistedToken.setExpirationDate(LocalDateTime.now());
      blacklistedToken.setBlacklistedAt(LocalDateTime.now());
      blacklistedToken.setReason("LOGOUT_EXPIRED_TOKEN");

      tokenBlacklistRepository.save(blacklistedToken);
      SecurityContextHolder.clearContext();

    } catch (JwtException e) {
      log.error("Error al procesar logout - Token inválido: {}", e.getMessage());
      throw new BusinessException("Token inválido para logout");
    } catch (Exception e) {
      log.error("Error inesperado durante logout: {}", e.getMessage(), e);
      throw new BusinessException("Error al cerrar sesión");
    }
  }

  // Métodos auxiliares privados

  private void registrarIntentoFallido(String email) {
    try {
      usuarioRepository.findByEmail(email).ifPresent(usuario -> {
        int intentos = usuario.getIntentosFallidos() + 1;
        usuario.setIntentosFallidos(intentos);

        // Bloquear usuario si excede el máximo de intentos
        if (intentos >= 5) { // Esto debería venir de configuración
          usuario.setBloqueado(true);
          log.warn("Usuario {} bloqueado por exceder intentos de login", email);
        }

        usuarioRepository.save(usuario);

        // Registrar evento de intento fallido
        AuditEvent failedEvent = new AuditEvent();
        failedEvent.setUsername(email);
        failedEvent.setEventType("LOGIN_FAILED");
        failedEvent.setEventDate(LocalDateTime.now());
        failedEvent.setIpAddress(getClientIpAddress());
        failedEvent.setUserAgent(getUserAgent());
        failedEvent.setDetails("Intento de login fallido. Intento #" + intentos);
        failedEvent.setSuccess(false);

        auditEventRepository.save(failedEvent);
      });
    } catch (Exception e) {
      log.error("Error registrando intento fallido: {}", e.getMessage());
    }
  }

  private String getClientIpAddress() {
    HttpServletRequest request = getCurrentRequest();
    if (request != null) {
      // Verificar headers de proxy
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
    return "unknown";
  }

  private String getUserAgent() {
    HttpServletRequest request = getCurrentRequest();
    return request != null ? request.getHeader("User-Agent") : "unknown";
  }

  private HttpServletRequest getCurrentRequest() {
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes instanceof ServletRequestAttributes) {
      return ((ServletRequestAttributes) requestAttributes).getRequest();
    }
    return null;
  }
}