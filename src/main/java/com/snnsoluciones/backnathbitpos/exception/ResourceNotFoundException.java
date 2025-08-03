package com.snnsoluciones.backnathbitpos.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción para cuando no se encuentra un recurso solicitado.
 * Retorna HTTP 404 NOT FOUND.
 */
@Getter
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

  private final String resourceName;
  private final String fieldName;
  private final Object fieldValue;

  public ResourceNotFoundException(String message) {
    super(message);
    this.resourceName = null;
    this.fieldName = null;
    this.fieldValue = null;
  }

  public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
    super(String.format("%s no encontrado con %s: '%s'", resourceName, fieldName, fieldValue));
    this.resourceName = resourceName;
    this.fieldName = fieldName;
    this.fieldValue = fieldValue;
  }

  public ResourceNotFoundException(String message, Throwable cause) {
    super(message, cause);
    this.resourceName = null;
    this.fieldName = null;
    this.fieldValue = null;
  }
}