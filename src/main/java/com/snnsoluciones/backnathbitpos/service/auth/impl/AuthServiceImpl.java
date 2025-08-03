package com.snnsoluciones.backnathbitpos.service.auth.impl;

import com.snnsoluciones.backnathbitpos.config.security.JwtTokenProvider;
import com.snnsoluciones.backnathbitpos.dto.request.LoginRequest;
import com.snnsoluciones.backnathbitpos.dto.response.LoginResponse;
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
import jakarta.persistence.Cache;
import jakarta.servlet.http.HttpServletRequest;
import java.time.ZoneId;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider tokenProvider;
  private final UsuarioRepository usuarioRepository;
  private final TokenBlacklistRepository tokenBlacklistRepository;
  private final AuditEventRepository auditEventRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final CacheManager cacheManager;

  @Value("${spring.security.jwt.expiration}")
  private Long jwtExpiration;

  @Override
  @Transactional
  public LoginResponse login(LoginRequest loginRequest) {
    log.info("Intento de login para: {}", loginRequest.getEmail());

    // Autenticar usuario
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            loginRequest.getEmail(),
            loginRequest.getPassword()
        )
    );

    SecurityContextHolder.getContext().setAuthentication(authentication);

    // Generar tokens
    String accessToken = tokenProvider.generateToken(authentication);
    String refreshToken = tokenProvider.generateRefreshToken(loginRequest.getEmail());

    // Obtener usuario y actualizar último acceso
    Usuario usuario = (Usuario) authentication.getPrincipal();
    usuario.setUltimoAcceso(LocalDateTime.now());
    usuario.setIntentosFallidos(0);
    usuarioRepository.save(usuario);

    // Construir respuesta
    return LoginResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .tokenType("Bearer")
        .expiresIn(jwtExpiration)
        .email(usuario.getEmail())
        .nombre(usuario.getNombre())
        .apellidos(usuario.getApellidos())
        .roles(usuario.getRol().getNombre().name())
        .tenantId(usuario.getTenantId())
        .build();
  }

  @Override
  public LoginResponse refreshToken(String refreshToken) {
    if (!tokenProvider.validateToken(refreshToken)) {
      throw new RuntimeException("Refresh token inválido");
    }

    String username = tokenProvider.getUsernameFromToken(refreshToken);
    String newAccessToken = tokenProvider.generateTokenFromUsername(username);

    Usuario usuario = usuarioRepository.findByEmail(username)
        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

    return LoginResponse.builder()
        .accessToken(newAccessToken)
        .refreshToken(refreshToken)
        .tokenType("Bearer")
        .expiresIn(jwtExpiration)
        .email(usuario.getEmail())
        .nombre(usuario.getNombre())
        .apellidos(usuario.getApellidos())
        .roles(usuario.getRol().getNombre().name())
        .tenantId(usuario.getTenantId())
        .build();
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
      Claims claims = jwtTokenProvider.validateToken(cleanToken);
      String username = claims.getSubject();
      Date expiration = claims.getExpiration();

      // 4. Agregar el token a la blacklist
      TokenBlacklist blacklistedToken = TokenBlacklist.builder()
          .token(cleanToken)
          .username(username)
          .expirationDate(expiration.toInstant()
              .atZone(ZoneId.systemDefault())
              .toLocalDateTime())
          .blacklistedAt(LocalDateTime.now())
          .reason("LOGOUT")
          .build();

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
      AuditEvent logoutEvent = AuditEvent.builder()
          .username(username)
          .eventType("LOGOUT")
          .eventDate(LocalDateTime.now())
          .ipAddress(getClientIpAddress())
          .userAgent(getUserAgent())
          .details("Usuario cerró sesión exitosamente")
          .success(true)
          .build();

      auditEventRepository.save(logoutEvent);

      // 8. Limpiar el contexto de seguridad
      SecurityContextHolder.clearContext();

      log.info("Usuario {} deslogueado exitosamente", username);

    } catch (ExpiredJwtException e) {
      // Si el token ya expiró, igual lo agregamos a la blacklist por seguridad
      log.warn("Intento de logout con token expirado: {}", e.getMessage());

      String username = e.getClaims().getSubject();
      TokenBlacklist blacklistedToken = TokenBlacklist.builder()
          .token(token)
          .username(username)
          .expirationDate(LocalDateTime.now())
          .blacklistedAt(LocalDateTime.now())
          .reason("LOGOUT_EXPIRED_TOKEN")
          .build();

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

  // Métodos auxiliares para obtener información del request
  private String getClientIpAddress() {
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes instanceof ServletRequestAttributes) {
      HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

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
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes instanceof ServletRequestAttributes) {
      HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
      return request.getHeader("User-Agent");
    }
    return "unknown";
  }
}