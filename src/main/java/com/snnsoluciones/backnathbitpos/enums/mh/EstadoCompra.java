package com.snnsoluciones.backnathbitpos.enums.mh;

/**
 * Estados del ciclo de vida de una COMPRA (factura recibida de proveedor)
 *
 * FLUJO NORMAL:
 * XML Subido → RECIBIDA → PENDIENTE_DECISION → ACEPTADA → COMPLETADA
 *
 * FLUJOS ALTERNATIVOS:
 * - RECHAZADA: Usuario rechazó la factura del proveedor
 * - ACEPTADA_PARCIAL: Usuario aceptó con ajuste de IVA
 * - ERROR_HACIENDA: Hacienda rechazó la factura original
 * - ERROR_PROCESO: Error interno procesando
 *
 * @author NathBit POS
 * @version 1.0
 */
public enum EstadoCompra {

    // ==================== ESTADOS INICIALES ====================

    /**
     * XML recibido y parseado correctamente.
     * Pendiente de validar con Hacienda.
     */
    RECIBIDA,

    /**
     * Validada con Hacienda exitosamente.
     * Esperando decisión del usuario (aceptar/rechazar/parcial).
     */
    PENDIENTE_DECISION,

    // ==================== ESTADOS FINALES POSITIVOS ====================

    /**
     * Usuario aceptó la factura completamente.
     * Mensaje receptor enviado a Hacienda.
     * Compra creada en el sistema.
     */
    ACEPTADA,

    /**
     * Usuario aceptó parcialmente (ajuste de IVA u otro monto).
     * Mensaje receptor de aceptación parcial enviado.
     * Compra creada con monto ajustado.
     */
    ACEPTADA_PARCIAL,

    /**
     * Compra completada.
     * Inventario actualizado exitosamente.
     * Métricas actualizadas.
     * Estado final del flujo normal.
     */
    COMPLETADA,

    // ==================== ESTADOS FINALES NEGATIVOS ====================

    /**
     * Usuario rechazó la factura del proveedor.
     * Mensaje receptor de rechazo enviado a Hacienda.
     * NO se crea compra.
     */
    RECHAZADA,

    /**
     * Hacienda rechazó la factura del proveedor.
     * NO se puede aceptar.
     * NO se crea compra.
     */
    ERROR_HACIENDA,

    /**
     * Error interno procesando la factura.
     * Ej: error parseando XML, error subiendo a S3, etc.
     */
    ERROR_PROCESO,

    // ==================== ESTADOS ESPECIALES ====================

    /**
     * Compra fue anulada posteriormente.
     * (Ej: devolución total, corrección, etc.)
     */
    ANULADA;

    // ==================== MÉTODOS HELPER ====================

    /**
     * Verifica si la compra puede ser editada
     */
    public boolean esEditable() {
        return this == RECIBIDA ||
            this == PENDIENTE_DECISION;
    }

    /**
     * Verifica si la compra está en un estado final
     */
    public boolean esFinal() {
        return this == COMPLETADA ||
            this == RECHAZADA ||
            this == ERROR_HACIENDA ||
            this == ANULADA;
    }

    /**
     * Verifica si la compra fue aceptada (total o parcial)
     */
    public boolean fueAceptada() {
        return this == ACEPTADA ||
            this == ACEPTADA_PARCIAL ||
            this == COMPLETADA;
    }

    /**
     * Verifica si la compra puede convertirse a registro de inventario
     */
    public boolean puedeAfectarInventario() {
        return this == ACEPTADA ||
            this == ACEPTADA_PARCIAL ||
            this == COMPLETADA;
    }

    /**
     * Obtiene descripción amigable para mostrar en UI
     */
    public String getDescripcion() {
        return switch (this) {
            case RECIBIDA -> "Factura recibida";
            case PENDIENTE_DECISION -> "Pendiente de decisión";
            case ACEPTADA -> "Aceptada";
            case ACEPTADA_PARCIAL -> "Aceptada parcialmente";
            case COMPLETADA -> "Completada";
            case RECHAZADA -> "Rechazada";
            case ERROR_HACIENDA -> "Error en Hacienda";
            case ERROR_PROCESO -> "Error de proceso";
            case ANULADA -> "Anulada";
        };
    }

    /**
     * Obtiene color para badge en UI
     * success, warning, danger, info, secondary
     */
    public String getColorBadge() {
        return switch (this) {
            case COMPLETADA -> "success";
            case ACEPTADA, ACEPTADA_PARCIAL -> "info";
            case PENDIENTE_DECISION, RECIBIDA -> "warning";
            case RECHAZADA, ERROR_HACIENDA, ERROR_PROCESO -> "danger";
            case ANULADA -> "secondary";
        };
    }
}