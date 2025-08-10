package com.snnsoluciones.backnathbitpos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    
    private String resourceName;
    private String fieldName;
    private Object fieldValue;
    
    // Constructor simple con solo mensaje
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    // Constructor con detalles del recurso
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s no encontrado con %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
    
    // Constructor para múltiples criterios
    public ResourceNotFoundException(String resourceName, String message) {
        super(String.format("%s no encontrado: %s", resourceName, message));
        this.resourceName = resourceName;
    }
    
    // Getters
    public String getResourceName() {
        return resourceName;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public Object getFieldValue() {
        return fieldValue;
    }
}