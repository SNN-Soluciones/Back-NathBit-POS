// enums/TipoFormaMesa.java
package com.snnsoluciones.backnathbitpos.enums;

public enum TipoFormaMesa {
    CUADRADA("Cuadrada"),
    RECTANGULAR("Rectangular"),
    REDONDA("Redonda");

    private final String descripcion;

    TipoFormaMesa(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}