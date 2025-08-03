package com.snnsoluciones.backnathbitpos.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción para errores relacionados con el sistema multi-tenant.
 * Se usa cuando hay problemas con el contexto del tenant o acceso no autorizado.
 */
@Getter
@ResponseStatus(HttpStatus.FORBIDDEN)
public class TenantException extends RuntimeException {

  private final String tenantId;
  private final String operation;

  public TenantException(String message) {
    super(message);
    this.tenantId = null;
    this.operation = null;
  }

  public TenantException(String message, String tenantId) {
    super(message);
    this.tenantId = tenantId;
    this.operation = null;
  }

  public TenantException(String message, String tenantId, String operation) {
    super(String.format("Error de tenant: %s. Tenant: %s, Operación: %s", message, tenantId, operation));
    this.tenantId = tenantId;
    this.operation = operation;
  }

  public TenantException(String message, Throwable cause) {
    super(message, cause);
    this.tenantId = null;
    this.operation = null;
  }

  public static TenantException notFound(String tenantId) {
    return new TenantException("Tenant no encontrado: " + tenantId, tenantId);
  }

  public static TenantException accessDenied(String tenantId) {
    return new TenantException("Acceso denegado al tenant: " + tenantId, tenantId);
  }

  public static TenantException notConfigured() {
    return new TenantException("No se ha configurado el tenant en el contexto actual");
  }
}