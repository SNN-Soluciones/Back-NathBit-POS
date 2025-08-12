package com.snnsoluciones.backnathbitpos.enums.facturacion;

/**
 * Estados del proceso asíncrono de facturación
 */
public enum EstadoProcesoJob {
    PENDIENTE("Pendiente de procesar"),
    PROCESANDO("En proceso"),
    COMPLETADO("Procesado exitosamente"),
    ERROR("Error en el procesamiento"),
    REINTENTANDO("Reintentando después de error"),
    CANCELADO("Cancelado por el usuario"),
    AUTORIZACION("Error de credenciales hacienda"),
    TIMEOUT("Tiempo de espera agotado");

    private final String descripcion;

    EstadoProcesoJob(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Indica si el job está en un estado final
     */
    public boolean esFinal() {
        return this == COMPLETADO || this == CANCELADO;
    }

    /**
     * Indica si el job puede ser reintentado
     */
    public boolean puedeReintentar() {
        return this == ERROR || this == TIMEOUT;
    }
}