package com.snnsoluciones.backnathbitpos.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción para conflictos de datos.
 * Se usa típicamente para violaciones de unicidad o cuando se intenta crear 
 * un recurso que ya existe. Retorna HTTP 409 CONFLICT.
 */
@Getter
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {
    
    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;
    
    public ConflictException(String message) {
        super(message);
        this.resourceName = null;
        this.fieldName = null;
        this.fieldValue = null;
    }
    
    public ConflictException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s ya existe con %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
    
    public ConflictException(String message, Throwable cause) {
        super(message, cause);
        this.resourceName = null;
        this.fieldName = null;
        this.fieldValue = null;
    }
    
    public static ConflictException duplicateEmail(String email) {
        return new ConflictException("Usuario", "email", email);
    }
    
    public static ConflictException duplicateIdentification(String identification) {
        return new ConflictException("Usuario", "identificación", identification);
    }
    
    public static ConflictException duplicateCode(String resourceName, String code) {
        return new ConflictException(resourceName, "código", code);
    }
}