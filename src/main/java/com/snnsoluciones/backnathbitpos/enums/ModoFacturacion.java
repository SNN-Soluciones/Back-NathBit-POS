package com.snnsoluciones.backnathbitpos.enums;

public enum ModoFacturacion {
    ELECTRONICO("Facturación Electrónica"),
    SOLO_INTERNO("Solo Control Interno");
    
    private final String descripcion;
    
    ModoFacturacion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
}