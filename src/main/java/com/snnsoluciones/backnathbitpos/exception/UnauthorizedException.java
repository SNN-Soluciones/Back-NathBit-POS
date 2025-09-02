package com.snnsoluciones.backnathbitpos.exception;

/**
 * Excepción para errores de autorización
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}