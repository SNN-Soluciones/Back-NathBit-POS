package com.snnsoluciones.backnathbitpos.enums;

public enum EstadoSesion {
    ABIERTA("Sesión Abierta"),
    CERRADA("Sesión Cerrada"),
    CANCELADA("Sesión Cancelada");
    
    private final String descripcion;
    
    EstadoSesion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
}