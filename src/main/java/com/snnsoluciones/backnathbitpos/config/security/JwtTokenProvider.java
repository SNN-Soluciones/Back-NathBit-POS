package com.snnsoluciones.backnathbitpos.config.security;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * Componente para manejar la generación, validación y parseo de tokens JWT.
 */
@Component
@Slf4j
public class JwtTokenProvider {
  @Value("${app.jwt.secret}")
  private String jwtSecret;

  @Value("${app.jwt.expiration:3600}")
  private Long jwtExpiration; // en segundos

  @Value("${app.jwt.refresh-expiration:604800}")
  private Long refreshExpiration; // 7 días en segundos

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Genera un token temporal para selección de contexto
   */
  public String generateTemporaryToken(String username) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + (300 * 1000)); // 5 minutos

    return Jwts.builder()
        .setSubject(username)
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .claim("type", "TEMPORARY")
        .claim("purpose", "CONTEXT_SELECTION")
        .signWith(getSigningKey(), SignatureAlgorithm.HS512)
        .compact();
  }

  /**
   * Genera un token con contexto completo
   */
  public String generateTokenWithFullContext(String username,
      UUID empresaId,
      UUID sucursalId,
      String tenantId,
      String rol) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + (jwtExpiration * 1000));

    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "ACCESS");
    claims.put("empresa_id", empresaId.toString());
    claims.put("sucursal_id", sucursalId.toString());
    claims.put("tenant_id", tenantId);
    claims.put("rol", rol);

    return Jwts.builder()
        .setClaims(claims)
        .setSubject(username)
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(getSigningKey(), SignatureAlgorithm.HS512)
        .compact();
  }

  /**
   * Genera un refresh token
   */
  public String generateRefreshToken(String username) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + (refreshExpiration * 1000));

    return Jwts.builder()
        .setSubject(username)
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .claim("type", "REFRESH")
        .signWith(getSigningKey(), SignatureAlgorithm.HS512)
        .compact();
  }

  /**
   * Obtiene el username del token
   */
  public String getUsernameFromToken(String token) {
    Claims claims = getClaims(token);
    return claims.getSubject();
  }

  /**
   * Obtiene el tenant del token
   */
  public String getTenantFromToken(String token) {
    Claims claims = getClaims(token);
    return claims.get("tenant_id", String.class);
  }

  /**
   * Valida si el token es válido
   */
  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder()
          .setSigningKey(getSigningKey())
          .build()
          .parseClaimsJws(token);
      return true;
    } catch (SecurityException ex) {
      log.error("Token JWT inválido - firma incorrecta", ex);
    } catch (MalformedJwtException ex) {
      log.error("Token JWT mal formado", ex);
    } catch (ExpiredJwtException ex) {
      log.error("Token JWT expirado", ex);
    } catch (UnsupportedJwtException ex) {
      log.error("Token JWT no soportado", ex);
    } catch (IllegalArgumentException ex) {
      log.error("JWT claims string está vacío", ex);
    }
    return false;
  }

  /**
   * Verifica si es un token temporal
   */
  public boolean isTemporaryToken(String token) {
    Claims claims = getClaims(token);
    String type = claims.get("type", String.class);
    return "TEMPORARY".equals(type);
  }

  /**
   * Verifica si es un refresh token
   */
  public boolean isRefreshToken(String token) {
    Claims claims = getClaims(token);
    String type = claims.get("type", String.class);
    return "REFRESH".equals(type);
  }

  /**
   * Obtiene todos los claims del token
   */
  private Claims getClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(getSigningKey())
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  /**
   * Genera un token desde la autenticación de Spring Security
   */
  public String generateTokenFromAuthentication(Authentication authentication) {
    String username = authentication.getName();

    // Extraer información adicional si está disponible
    Map<String, Object> details = new HashMap<>();
    if (authentication.getDetails() instanceof Map) {
      details = (Map<String, Object>) authentication.getDetails();
    }

    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + (jwtExpiration * 1000));

    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "ACCESS");
    claims.put("authorities", authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toList()));

    // Agregar detalles adicionales si existen
    if (details.containsKey("empresa_id")) {
      claims.put("empresa_id", details.get("empresa_id"));
    }
    if (details.containsKey("sucursal_id")) {
      claims.put("sucursal_id", details.get("sucursal_id"));
    }
    if (details.containsKey("tenant_id")) {
      claims.put("tenant_id", details.get("tenant_id"));
    }

    return Jwts.builder()
        .setClaims(claims)
        .setSubject(username)
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(getSigningKey(), SignatureAlgorithm.HS512)
        .compact();
  }

  /**
   * Invalida un token (para logout)
   * Nota: En una implementación real, esto debería guardar el token en una blacklist
   */
  public void invalidateToken(String token) {
    // TODO: Implementar blacklist de tokens
    log.info("Token invalidado para logout");
  }

  /**
   * Obtiene el tiempo de expiración del token
   */
  public Date getExpirationDateFromToken(String token) {
    Claims claims = getClaims(token);
    return claims.getExpiration();
  }

  /**
   * Verifica si el token está próximo a expirar (menos de 5 minutos)
   */
  public boolean isTokenAboutToExpire(String token) {
    Date expiration = getExpirationDateFromToken(token);
    long timeUntilExpiration = expiration.getTime() - System.currentTimeMillis();
    return timeUntilExpiration < (5 * 60 * 1000); // 5 minutos
  }

  // Agregar estos métodos a JwtTokenProvider.java:

  /**
   * Genera un token con contexto completo usando IDs Long
   */
  public String generateToken(Long usuarioId, String username, Long empresaId, Long sucursalId, RolNombre rol) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + (jwtExpiration * 1000));

    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "ACCESS");
    claims.put("usuario_id", usuarioId);
    claims.put("empresa_id", empresaId);
    claims.put("sucursal_id", sucursalId);
    claims.put("rol", rol.name());

    return Jwts.builder()
        .setClaims(claims)
        .setSubject(username)
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(getSigningKey(), SignatureAlgorithm.HS512)
        .compact();
  }

  /**
   * Genera un refresh token usando el ID del usuario
   */
  public String generateRefreshToken(Long usuarioId) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + (refreshExpiration * 1000));

    return Jwts.builder()
        .setSubject(usuarioId.toString())
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .claim("type", "REFRESH")
        .claim("usuario_id", usuarioId)
        .signWith(getSigningKey(), SignatureAlgorithm.HS512)
        .compact();
  }

  /**
   * Obtiene el ID del usuario del token
   */
  public Long getUserIdFromToken(String token) {
    Claims claims = getClaims(token);
    // Primero intentar obtener de usuario_id, si no del subject
    Object usuarioId = claims.get("usuario_id");
    if (usuarioId != null) {
      return Long.valueOf(usuarioId.toString());
    }
    // Si no hay usuario_id, intentar parsear el subject
    String subject = claims.getSubject();
    if (subject != null && subject.matches("\\d+")) {
      return Long.parseLong(subject);
    }
    return null;
  }

  /**
   * Obtiene el ID de empresa del token como Long
   */
  public Long getEmpresaIdFromToken(String token) {
    Claims claims = getClaims(token);
    Object empresaId = claims.get("empresa_id");
    return empresaId != null ? Long.valueOf(empresaId.toString()) : null;
  }

  /**
   * Obtiene el ID de sucursal del token como Long
   */
  public Long getSucursalIdFromToken(String token) {
    Claims claims = getClaims(token);
    Object sucursalId = claims.get("sucursal_id");
    return sucursalId != null ? Long.valueOf(sucursalId.toString()) : null;
  }

  /**
   * Obtiene el rol del token como RolNombre
   */
  public RolNombre getRolFromToken(String token) {
    Claims claims = getClaims(token);
    String rol = claims.get("rol", String.class);
    return rol != null ? RolNombre.valueOf(rol) : null;
  }

  /**
   * Obtiene el tiempo de expiración en segundos
   */
  public Long getExpirationTime() {
    return jwtExpiration;
  }

}