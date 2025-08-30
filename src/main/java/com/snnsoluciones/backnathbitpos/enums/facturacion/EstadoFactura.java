package com.snnsoluciones.backnathbitpos.enums.facturacion;

/**
 * Estados posibles de una factura en el sistema
 */
public enum EstadoFactura {
    GENERADA("Factura generada y lista para procesar"),
    PROCESANDO("En proceso de firma y envío a Hacienda"),
    FIRMADA("Documento firmado digitalmente"),
    ENVIADA("Enviada a Hacienda, esperando respuesta"),
    ACEPTADA("Aceptada por Hacienda"),
    RECHAZADA("Rechazada por Hacienda"),
    ANULADA("Anulada por el usuario"),
    ERROR("Error en el procesamiento"),
    NOTIFICADA("Notificada al cliente"),
    CANCELADA("Cancelada por el usuario"); // <-- NUEVO ESTADO

    private final String descripcion;

    EstadoFactura(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Valida si la factura puede ser anulada
     */
    public boolean puedeAnularse() {
        return this == GENERADA || this == ACEPTADA;
    }

    /**
     * Valida si la factura puede ser reprocesada
     */
    public boolean puedeReprocesarse() {
        return this == ERROR || this == RECHAZADA;
    }
}