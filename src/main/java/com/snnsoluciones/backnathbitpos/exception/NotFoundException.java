package com.snnsoluciones.backnathbitpos.exception;

/**
 * Excepción para cuando no se encuentra un recurso
 */
public class NotFoundException extends RuntimeException {
    
    public NotFoundException(String message) {
        super(message);
    }
    
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}