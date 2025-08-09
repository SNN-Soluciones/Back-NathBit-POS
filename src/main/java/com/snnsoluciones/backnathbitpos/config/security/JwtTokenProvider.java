package com.snnsoluciones.backnathbitpos.security;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Proveedor de tokens JWT para autenticación
 */
@Slf4j
@Component
public class JwtTokenProvider {

  @Value("${app.jwt.secret}")
  private String jwtSecret;

  @Value("${app.jwt.expiration:3600}") // 1 hora por defecto
  private int jwtExpiration;

  @Value("${app.jwt.refresh-expiration:604800}") // 7 días por defecto
  private int jwtRefreshExpiration;

  /**
   * Genera un token de acceso básico desde Authentication
   */
  public String generateToken(Authentication authentication) {
    UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + (jwtExpiration * 1000L));

    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "ACCESS");

    return Jwts.builder()
        .setClaims(claims)
        .setSubject(userPrincipal.getUsername())
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(getSigningKey(), SignatureAlgorithm.HS512)
        .compact();
  }

  /**
   * Genera un token con contexto completo
   */
  public String generateToken(Long usuarioId, String username, Long empresaId,
      Long sucursalId, RolNombre rol) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + (jwtExpiration * 1000L));

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
   * Genera un refresh token
   */
  public String generateRefreshToken(Authentication authentication) {
    UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + (jwtRefreshExpiration * 1000L));

    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "REFRESH");

    return Jwts.builder()
        .setClaims(claims)
        .setSubject(userPrincipal.getUsername())
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(getSigningKey(), SignatureAlgorithm.HS512)
        .compact();
  }

  /**
   * Genera un refresh token con usuario ID
   */
  public String generateRefreshToken(Long usuarioId, String username) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + (jwtRefreshExpiration * 1000L));

    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "REFRESH");
    claims.put("usuario_id", usuarioId);

    return Jwts.builder()
        .setClaims(claims)
        .setSubject(username)
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(getSigningKey(), SignatureAlgorithm.HS512)
        .compact();
  }

  /**
   * Genera un token con claims personalizados
   */
  public String generateTokenWithClaims(Authentication authentication, Map<String, Object> additionalClaims) {
    UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + (jwtExpiration * 1000L));

    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "ACCESS");
    claims.putAll(additionalClaims);

    return Jwts.builder()
        .setClaims(claims)
        .setSubject(userPrincipal.getUsername())
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(getSigningKey(), SignatureAlgorithm.HS512)
        .compact();
  }

  /**
   * Obtiene el username del token
   */
  public String getUsernameFromToken(String token) {
    Claims claims = getAllClaimsFromToken(token);
    return claims.getSubject();
  }

  /**
   * Obtiene el ID del usuario del token
   */
  public Long getUserIdFromToken(String token) {
    Claims claims = getAllClaimsFromToken(token);
    return claims.get("usuario_id", Long.class);
  }

  /**
   * Obtiene el ID de la empresa del token
   */
  public Long getEmpresaIdFromToken(String token) {
    Claims claims = getAllClaimsFromToken(token);
    return claims.get("empresa_id", Long.class);
  }

  /**
   * Obtiene el ID de la sucursal del token
   */
  public Long getSucursalIdFromToken(String token) {
    Claims claims = getAllClaimsFromToken(token);
    return claims.get("sucursal_id", Long.class);
  }

  /**
   * Obtiene el rol del token
   */
  public String getRolFromToken(String token) {
    Claims claims = getAllClaimsFromToken(token);
    return claims.get("rol", String.class);
  }

  /**
   * Obtiene el tipo de token
   */
  public String getTokenType(String token) {
    Claims claims = getAllClaimsFromToken(token);
    return claims.get("type", String.class);
  }

  /**
   * Valida el token
   */
  public boolean validateToken(String token) {
    try {
      getAllClaimsFromToken(token);
      return true;
    } catch (SecurityException ex) {
      log.error("Token JWT tiene firma inválida");
    } catch (MalformedJwtException ex) {
      log.error("Token JWT inválido");
    } catch (ExpiredJwtException ex) {
      log.error("Token JWT expirado");
    } catch (UnsupportedJwtException ex) {
      log.error("Token JWT no soportado");
    } catch (IllegalArgumentException ex) {
      log.error("JWT claims string está vacío");
    }
    return false;
  }

  /**
   * Verifica si el token está expirado
   */
  public boolean isTokenExpired(String token) {
    try {
      Claims claims = getAllClaimsFromToken(token);
      Date expiration = claims.getExpiration();
      return expiration.before(new Date());
    } catch (ExpiredJwtException e) {
      return true;
    }
  }

  /**
   * Obtiene el tiempo de expiración en segundos
   */
  public long getExpirationTime() {
    return jwtExpiration;
  }

  /**
   * Obtiene todos los claims del token
   */
  private Claims getAllClaimsFromToken(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(getSigningKey())
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  /**
   * Obtiene la llave de firma
   */
  private Key getSigningKey() {
    byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}