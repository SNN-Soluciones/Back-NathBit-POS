package com.snnsoluciones.backnathbitpos.enums;

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
}