package com.snnsoluciones.backnathbitpos.enums;

public enum AmbienteHacienda {
    SANDBOX("Ambiente de Pruebas"),
    PRODUCCION("Ambiente de Producción");
    
    private final String descripcion;
    
    AmbienteHacienda(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
}