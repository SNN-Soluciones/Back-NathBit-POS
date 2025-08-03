package com.snnsoluciones.backnathbitpos.config.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
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

  @Value("${jwt.secret}")  // Cambiado de spring.security.jwt.secret
  private String jwtSecret;

  @Value("${jwt.expiration}")  // Cambiado de spring.security.jwt.expiration
  private Long jwtExpiration;

  @Value("${jwt.refresh-expiration}")  // Cambiado de spring.security.jwt.refresh-expiration
  private Long refreshExpiration;

  /**
   * Genera un token JWT a partir de la autenticación.
   */
  public String generateToken(Authentication authentication) {
    UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
    return generateTokenFromUsername(userPrincipal.getUsername());
  }

  /**
   * Genera un token JWT a partir del nombre de usuario.
   */
  public String generateTokenFromUsername(String username) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtExpiration);

    return Jwts.builder()
        .setSubject(username)
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(getSignKey(), SignatureAlgorithm.HS512)
        .compact();
  }

  /**
   * Genera un refresh token con mayor duración.
   */
  public String generateRefreshToken(String username) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + refreshExpiration);

    return Jwts.builder()
        .setSubject(username)
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .claim("type", "refresh")
        .signWith(getSignKey(), SignatureAlgorithm.HS512)
        .compact();
  }

  /**
   * Obtiene el nombre de usuario del token.
   */
  public String getUsernameFromToken(String token) {
    Claims claims = Jwts.parserBuilder()
        .setSigningKey(getSignKey())
        .build()
        .parseClaimsJws(token)
        .getBody();

    return claims.getSubject();
  }

  /**
   * Valida el token y retorna true si es válido.
   */
  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder()
          .setSigningKey(getSignKey())
          .build()
          .parseClaimsJws(token);
      return true;
    } catch (SecurityException ex) {
      log.error("Token JWT inválido - firma no válida");
    } catch (MalformedJwtException ex) {
      log.error("Token JWT inválido - formato incorrecto");
    } catch (ExpiredJwtException ex) {
      log.error("Token JWT expirado");
    } catch (UnsupportedJwtException ex) {
      log.error("Token JWT no soportado");
    } catch (IllegalArgumentException ex) {
      log.error("JWT claims vacío");
    }
    return false;
  }

  /**
   * Valida el token y retorna los claims si es válido.
   * Lanza excepción si el token no es válido.
   */
  public Claims validateAndGetClaims(String token) {
    try {
      return Jwts.parserBuilder()
          .setSigningKey(getSignKey())
          .build()
          .parseClaimsJws(token)
          .getBody();
    } catch (ExpiredJwtException e) {
      // Re-lanzamos para que el servicio pueda manejar tokens expirados especialmente
      throw e;
    } catch (SecurityException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException ex) {
      log.error("Error validando token: {}", ex.getMessage());
      throw new JwtException("Token inválido");
    }
  }

  /**
   * Verifica si el token está próximo a expirar (menos de 5 minutos).
   */
  public boolean isTokenAboutToExpire(String token) {
    try {
      Claims claims = Jwts.parserBuilder()
          .setSigningKey(getSignKey())
          .build()
          .parseClaimsJws(token)
          .getBody();

      Date expiration = claims.getExpiration();
      Date now = new Date();
      long diff = expiration.getTime() - now.getTime();

      // Si quedan menos de 5 minutos
      return diff < 300000;
    } catch (Exception e) {
      return true;
    }
  }

  /**
   * Obtiene la fecha de expiración del token.
   */
  public Date getExpirationDateFromToken(String token) {
    Claims claims = Jwts.parserBuilder()
        .setSigningKey(getSignKey())
        .build()
        .parseClaimsJws(token)
        .getBody();

    return claims.getExpiration();
  }

  /**
   * Verifica si es un refresh token.
   */
  public boolean isRefreshToken(String token) {
    try {
      Claims claims = Jwts.parserBuilder()
          .setSigningKey(getSignKey())
          .build()
          .parseClaimsJws(token)
          .getBody();

      String type = (String) claims.get("type");
      return "refresh".equals(type);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Genera un token JWT con información del tenant
   */
  public String generateTokenWithTenant(String username, String tenantId, String role) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtExpiration);

    return Jwts.builder()
        .setSubject(username)
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .claim("tenantId", tenantId)
        .claim("role", role)
        .signWith(getSignKey(), SignatureAlgorithm.HS512)
        .compact();
  }

  /**
   * Invalida un token (se debe usar con un servicio de blacklist)
   */
  public void invalidateToken(String token) {
    // Este método normalmente se usa con un servicio de blacklist
    // Por ahora solo logueamos la invalidación
    log.info("Token invalidado: {}", token.substring(0, 20) + "...");
  }

  /**
   * Obtiene el username de un refresh token
   */
  public String getUsernameFromRefreshToken(String refreshToken) {
    return getUsernameFromToken(refreshToken);
  }

  /**
   * Obtiene el tenant ID del token
   */
  public String getTenantFromToken(String token) {
    try {
      Claims claims = Jwts.parserBuilder()
          .setSigningKey(getSignKey())
          .build()
          .parseClaimsJws(token)
          .getBody();
      return (String) claims.get("tenantId");
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Obtiene el tenant ID de un refresh token
   */
  public String getTenantFromRefreshToken(String refreshToken) {
    return getTenantFromToken(refreshToken);
  }

  /**
   * Obtiene el rol del refresh token
   */
  public String getRoleFromRefreshToken(String refreshToken) {
    try {
      Claims claims = Jwts.parserBuilder()
          .setSigningKey(getSignKey())
          .build()
          .parseClaimsJws(refreshToken)
          .getBody();
      return (String) claims.get("role");
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Genera un token sin información de tenant
   */
  public String generateTokenWithoutTenant(String username) {
    return generateTokenFromUsername(username);
  }

  /**
   * Valida un refresh token
   */
  public boolean validateRefreshToken(String refreshToken) {
    return validateToken(refreshToken) && isRefreshToken(refreshToken);
  }

  /**
   * Obtiene la clave de firma decodificada.
   */
  private Key getSignKey() {
    byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}