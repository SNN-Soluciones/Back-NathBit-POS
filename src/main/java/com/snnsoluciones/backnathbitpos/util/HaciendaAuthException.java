package com.snnsoluciones.backnathbitpos.util;

// Excepción custom para 403
public class HaciendaAuthException extends RuntimeException {
    public HaciendaAuthException(String message) {
        super(message);
    }
}
