package com.snnsoluciones.backnathbitpos.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para la aplicación.
 * Intercepta las excepciones y devuelve respuestas consistentes.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * Maneja BusinessException
   */
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(
      BusinessException ex, WebRequest request) {
    log.error("Error de negocio: {}", ex.getMessage());

    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setTimestamp(LocalDateTime.now());
    errorResponse.setStatus(ex.getStatus().value());
    errorResponse.setError(ex.getStatus().getReasonPhrase());
    errorResponse.setMessage(ex.getMessage());
    errorResponse.setCode(ex.getCode());
    errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

    return new ResponseEntity<>(errorResponse, ex.getStatus());
  }

  /**
   * Maneja ResourceNotFoundException
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
      ResourceNotFoundException ex, WebRequest request) {
    log.error("Recurso no encontrado: {}", ex.getMessage());

    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setTimestamp(LocalDateTime.now());
    errorResponse.setStatus(HttpStatus.NOT_FOUND.value());
    errorResponse.setError("Recurso no encontrado");
    errorResponse.setMessage(ex.getMessage());
    errorResponse.setCode("RESOURCE_NOT_FOUND");
    errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }

  /**
   * Maneja TenantException
   */
  @ExceptionHandler(TenantException.class)
  public ResponseEntity<ErrorResponse> handleTenantException(
      TenantException ex, WebRequest request) {
    log.error("Error de tenant: {}", ex.getMessage());

    Map<String, Object> details = new HashMap<>();
    if (ex.getTenantId() != null) {
      details.put("tenantId", ex.getTenantId());
    }
    if (ex.getOperation() != null) {
      details.put("operation", ex.getOperation());
    }

    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setTimestamp(LocalDateTime.now());
    errorResponse.setStatus(HttpStatus.FORBIDDEN.value());
    errorResponse.setError("Error de Tenant");
    errorResponse.setMessage(ex.getMessage());
    errorResponse.setCode("TENANT_ERROR");
    errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
    errorResponse.setDetails(details);

    return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
  }

  /**
   * Maneja errores de validación de @Valid
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(
      MethodArgumentNotValidException ex, WebRequest request) {
    log.error("Error de validación: {}", ex.getMessage());

    Map<String, String> errors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            FieldError::getDefaultMessage,
            (existing, replacement) -> existing
        ));

    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setTimestamp(LocalDateTime.now());
    errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
    errorResponse.setError("Error de validación");
    errorResponse.setMessage("Los datos enviados no son válidos");
    errorResponse.setCode("VALIDATION_ERROR");
    errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
    errorResponse.setDetails(errors);

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /**
   * Maneja errores de validación de constraints
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolationException(
      ConstraintViolationException ex, WebRequest request) {
    log.error("Error de constraint: {}", ex.getMessage());

    Map<String, String> errors = ex.getConstraintViolations()
        .stream()
        .collect(Collectors.toMap(
            violation -> violation.getPropertyPath().toString(),
            ConstraintViolation::getMessage,
            (existing, replacement) -> existing
        ));

    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setTimestamp(LocalDateTime.now());
    errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
    errorResponse.setError("Error de validación");
    errorResponse.setMessage("Violación de restricciones");
    errorResponse.setCode("CONSTRAINT_VIOLATION");
    errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
    errorResponse.setDetails(errors);

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /**
   * Maneja errores de integridad de datos
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
      DataIntegrityViolationException ex, WebRequest request) {
    log.error("Error de integridad de datos: {}", ex.getMessage());

    String message = "Error de integridad de datos";
    if (ex.getMessage().contains("duplicate key")) {
      message = "El registro ya existe";
    } else if (ex.getMessage().contains("foreign key")) {
      message = "El registro está relacionado con otros datos";
    }

    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setTimestamp(LocalDateTime.now());
    errorResponse.setStatus(HttpStatus.CONFLICT.value());
    errorResponse.setError("Conflicto de datos");
    errorResponse.setMessage(message);
    errorResponse.setCode("DATA_INTEGRITY_ERROR");
    errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }

  /**
   * Maneja errores de autenticación
   */
  @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
  public ResponseEntity<ErrorResponse> handleAuthenticationException(
      Exception ex, WebRequest request) {
    log.error("Error de autenticación: {}", ex.getMessage());

    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.UNAUTHORIZED,
        "Credenciales inválidas",
        "AUTHENTICATION_ERROR"
    );
    errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

    return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
  }

  /**
   * Maneja cuentas deshabilitadas o bloqueadas
   */
  @ExceptionHandler({DisabledException.class, LockedException.class})
  public ResponseEntity<ErrorResponse> handleAccountStatusException(
      Exception ex, WebRequest request) {
    log.error("Error de estado de cuenta: {}", ex.getMessage());

    String message = ex instanceof DisabledException ?
        "La cuenta está deshabilitada" : "La cuenta está bloqueada";

    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.FORBIDDEN,
        message,
        "ACCOUNT_STATUS_ERROR"
    );
    errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

    return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
  }

  /**
   * Maneja errores de acceso denegado
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(
      AccessDeniedException ex, WebRequest request) {
    log.error("Acceso denegado: {}", ex.getMessage());

    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.FORBIDDEN,
        "No tiene permisos para realizar esta acción",
        "ACCESS_DENIED"
    );
    errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

    return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
  }

  /**
   * Maneja errores de tipo de argumento incorrecto
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatchException(
      MethodArgumentTypeMismatchException ex, WebRequest request) {
    log.error("Error de tipo de argumento: {}", ex.getMessage());

    String message = String.format("El parámetro '%s' debe ser de tipo %s",
        ex.getName(), ex.getRequiredType().getSimpleName());

    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.BAD_REQUEST,
        message,
        "TYPE_MISMATCH"
    );
    errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /**
   * Maneja cualquier otra excepción no manejada
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGlobalException(
      Exception ex, WebRequest request) {
    log.error("Error no manejado: ", ex);

    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Ha ocurrido un error inesperado",
        "INTERNAL_ERROR"
    );
    errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}