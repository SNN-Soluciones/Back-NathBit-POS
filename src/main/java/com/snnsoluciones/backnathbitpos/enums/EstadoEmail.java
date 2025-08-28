package com.snnsoluciones.backnathbitpos.enums;

/**
 * Estados posibles de envío de email
 */
public enum EstadoEmail {
    PENDIENTE("Pendiente de envío"),
    ENVIADO("Enviado exitosamente"),
    ERROR("Error en el envío"),
    REINTENTANDO("En proceso de reintento"),
    FALLO_PERMANENTE("Fallo permanente - No reintentar");

    private final String descripcion;

    EstadoEmail(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Indica si el estado permite reintentos
     */
    public boolean permiteReintento() {
        return this == ERROR || this == REINTENTANDO;
    }
}