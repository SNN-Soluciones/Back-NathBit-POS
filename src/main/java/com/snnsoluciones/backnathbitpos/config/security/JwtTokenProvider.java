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

@Component
@Slf4j
public class JwtTokenProvider {

  @Value("${spring.security.jwt.secret}")
  private String jwtSecret;

  @Value("${spring.security.jwt.expiration}")
  private Long jwtExpiration;

  @Value("${spring.security.jwt.refresh-expiration}")
  private Long refreshExpiration;

  /**
   * Genera un token JWT para el usuario autenticado
   */
  public String generateToken(Authentication authentication) {
    UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
    return generateTokenFromUsername(userPrincipal.getUsername());
  }

  /**
   * Genera un token JWT desde el username
   */
  public String generateTokenFromUsername(String username) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtExpiration);

    return Jwts.builder()
        .setSubject(username)
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(key(), SignatureAlgorithm.HS256)
        .compact();
  }

  /**
   * Genera un refresh token
   */
  public String generateRefreshToken(String username) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + refreshExpiration);

    return Jwts.builder()
        .setSubject(username)
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .claim("type", "refresh")
        .signWith(key(), SignatureAlgorithm.HS256)
        .compact();
  }

  /**
   * Obtiene el username del token
   */
  public String getUsernameFromToken(String token) {
    Claims claims = Jwts.parserBuilder()
        .setSigningKey(key())
        .build()
        .parseClaimsJws(token)
        .getBody();

    return claims.getSubject();
  }

  /**
   * Valida si el token es válido
   */
  public boolean validateToken(String authToken) {
    try {
      Jwts.parserBuilder()
          .setSigningKey(key())
          .build()
          .parseClaimsJws(authToken);
      return true;
    } catch (SecurityException ex) {
      log.error("Token JWT inválido - firma incorrecta");
    } catch (MalformedJwtException ex) {
      log.error("Token JWT inválido - formato incorrecto");
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
   * Obtiene la fecha de expiración del token
   */
  public Date getExpirationDateFromToken(String token) {
    Claims claims = Jwts.parserBuilder()
        .setSigningKey(key())
        .build()
        .parseClaimsJws(token)
        .getBody();

    return claims.getExpiration();
  }

  /**
   * Genera la key para firmar los tokens
   */
  private Key key() {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
  }
}