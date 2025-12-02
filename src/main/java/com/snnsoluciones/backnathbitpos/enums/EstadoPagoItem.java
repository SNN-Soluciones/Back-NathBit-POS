package com.snnsoluciones.backnathbitpos.enums;

/**
 * Estado de pago de un item dentro de una orden
 * Usado para pagos parciales en mesas de restaurante
 */
public enum EstadoPagoItem {
    PENDIENTE("Pendiente", "Item aún no ha sido pagado"),
    PAGADO("Pagado", "Item completamente pagado"),
    PARCIAL("Parcial", "Item parcialmente pagado (división equitativa)");

    private final String descripcion;
    private final String detalle;

    EstadoPagoItem(String descripcion, String detalle) {
        this.descripcion = descripcion;
        this.detalle = detalle;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getDetalle() {
        return detalle;
    }
}