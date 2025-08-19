package com.snnsoluciones.backnathbitpos.enums.mh;

import java.util.Arrays;
import java.util.Optional;

public enum SituacionDocumento {
    NORMAL("1", "Normal"),
    CONTINGENCIA("2", "Contingencia"),
    SIN_INTERNET("3", "Sin Internet");
    
    private final String codigo;
    private final String descripcion;
    
    SituacionDocumento(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }
    
    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }

    public static Optional<?> fromCodigoOptional(String codigo) {
        if (codigo == null) return Optional.empty();

        return Arrays.stream(values())
            .filter(tipo -> tipo.codigo.equals(codigo))
            .findFirst();
    }

    public static SituacionDocumento fromCodigo(String codigo) {
        return Arrays.stream(values())
            .filter(cv -> cv.getCodigo().equals(codigo))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Código de condición venta no válido: " + codigo));
    }
}