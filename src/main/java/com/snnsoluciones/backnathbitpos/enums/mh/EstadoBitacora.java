package com.snnsoluciones.backnathbitpos.enums.mh;

/**
 * Enum para EstadoBitacora
 */
public enum EstadoBitacora {
    PENDIENTE("Pendiente de procesar"),
    PROCESANDO("Procesando actualmente"),
    ACEPTADA("Aceptada por Hacienda"),
    RECHAZADA("Rechazada por Hacienda"),
    ERROR("Error en el proceso"),
    CANCELADO("Cancelado por el usuario");
    
    private final String descripcion;
    
    EstadoBitacora(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
}