package com.snnsoluciones.backnathbitpos.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción para solicitudes mal formadas o con datos inválidos.
 * Se usa cuando los datos enviados no cumplen con las validaciones o 
 * reglas de negocio. Retorna HTTP 400 BAD REQUEST.
 */
@Getter
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {
    
    private final String code;
    private final Object details;
    
    public BadRequestException(String message) {
        super(message);
        this.code = "BAD_REQUEST";
        this.details = null;
    }
    
    public BadRequestException(String message, String code) {
        super(message);
        this.code = code;
        this.details = null;
    }
    
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
        this.code = "BAD_REQUEST";
        this.details = null;
    }
    
    public BadRequestException(String message, String code, Object details) {
        super(message);
        this.code = code;
        this.details = details;
    }
    
    public static BadRequestException invalidPassword() {
        return new BadRequestException("La contraseña actual es incorrecta", "INVALID_PASSWORD");
    }
    
    public static BadRequestException invalidData(String field) {
        return new BadRequestException(
            String.format("El campo '%s' contiene datos inválidos", field),
            "INVALID_DATA"
        );
    }
    
    public static BadRequestException missingField(String field) {
        return new BadRequestException(
            String.format("El campo '%s' es requerido", field),
            "MISSING_FIELD"
        );
    }
}