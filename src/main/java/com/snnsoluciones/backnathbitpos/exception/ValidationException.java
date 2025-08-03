package com.snnsoluciones.backnathbitpos.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Excepción para errores de validación personalizados.
 * Permite incluir múltiples errores de campo.
 */
@Getter
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public ValidationException(String message) {
        super(message);
        this.errors = new HashMap<>();
    }

    public ValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }

    public ValidationException(String field, String error) {
        super("Error de validación");
        this.errors = new HashMap<>();
        this.errors.put(field, error);
    }

    public void addError(String field, String error) {
        this.errors.put(field, error);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}