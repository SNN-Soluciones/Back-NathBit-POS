// src/main/java/com/snnsoluciones/backnathbitpos/enums/MetodoImpresion.java
package com.snnsoluciones.backnathbitpos.enums;

import lombok.Getter;

@Getter
public enum MetodoImpresion {
    AUTO("Automático - Detectar dispositivo"),
    IFRAME("IFrame - Impresión directa"),
    SHARE_API("Share API - Menú compartir"),
    NUEVA_PESTANA("Nueva Pestaña");

    private final String descripcion;

    MetodoImpresion(String descripcion) {
        this.descripcion = descripcion;
    }
}