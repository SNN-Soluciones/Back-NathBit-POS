package com.snnsoluciones.backnathbitpos.enums;

public enum EstadoCuenta {
    VIGENTE("Vigente"),
    VENCIDA("Vencida"), 
    PAGADA("Pagada"),
    PARCIAL("Parcialmente Pagada");
    
    private final String descripcion;
    
    EstadoCuenta(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getDescripcion() { 
        return descripcion; 
    }
}