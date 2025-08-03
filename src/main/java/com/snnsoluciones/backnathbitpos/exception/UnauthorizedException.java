package com.snnsoluciones.backnathbitpos.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción para errores de autorización.
 * Se usa cuando el usuario no está autenticado o el token es inválido.
 */
@Getter
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {

  private final String reason;

  public UnauthorizedException(String message) {
    super(message);
    this.reason = "UNAUTHORIZED";
  }

  public UnauthorizedException(String message, String reason) {
    super(message);
    this.reason = reason;
  }

  public static UnauthorizedException tokenExpired() {
    return new UnauthorizedException("El token ha expirado", "TOKEN_EXPIRED");
  }

  public static UnauthorizedException invalidToken() {
    return new UnauthorizedException("Token inválido", "INVALID_TOKEN");
  }

  public static UnauthorizedException missingToken() {
    return new UnauthorizedException("Token no encontrado", "MISSING_TOKEN");
  }

  public static UnauthorizedException invalidCredentials() {
    return new UnauthorizedException("Credenciales inválidas", "INVALID_CREDENTIALS");
  }
}