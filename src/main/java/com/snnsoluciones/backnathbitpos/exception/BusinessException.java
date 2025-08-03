package com.snnsoluciones.backnathbitpos.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción para errores de lógica de negocio.
 * Se usa cuando una operación no puede completarse por reglas de negocio.
 */
@Getter
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessException extends RuntimeException {

  private final String code;
  private final HttpStatus status;

  public BusinessException(String message) {
    super(message);
    this.code = "BUSINESS_ERROR";
    this.status = HttpStatus.BAD_REQUEST;
  }

  public BusinessException(String message, String code) {
    super(message);
    this.code = code;
    this.status = HttpStatus.BAD_REQUEST;
  }

  public BusinessException(String message, Throwable cause) {
    super(message, cause);
    this.code = "BUSINESS_ERROR";
    this.status = HttpStatus.BAD_REQUEST;
  }

  public BusinessException(String message, String code, HttpStatus status) {
    super(message);
    this.code = code;
    this.status = status;
  }
}