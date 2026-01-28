// enums/TipoFormaBarra.java
package com.snnsoluciones.backnathbitpos.enums;

public enum TipoFormaBarra {
    LINEA_RECTA("Línea Recta"),
    CODO_L("Codo en L"),
    CODO_U("Codo en U"),
    CIRCULO("Círculo (Isla)"),
    CURVA("Curva Semicircular");

    private final String descripcion;

    TipoFormaBarra(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}