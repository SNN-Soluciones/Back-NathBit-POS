// enums/EstadoSillaBarra.java
package com.snnsoluciones.backnathbitpos.enums;

public enum EstadoSillaBarra {
    DISPONIBLE("Disponible"),
    OCUPADA("Ocupada"),
    RESERVADA("Reservada");

    private final String descripcion;

    EstadoSillaBarra(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}