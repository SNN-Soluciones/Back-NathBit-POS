package com.snnsoluciones.backnathbitpos.enums.mh;

/**
 * Estados del ciclo de vida de una COMPRA (factura recibida de proveedor)
 *
 * FLUJO NORMAL:
 * 1. RECIBIDA → Parseo del XML exitoso, guardada en BD
 * 2. VALIDADA_HACIENDA → Consultamos a Hacienda y está aceptada
 * 3. PENDIENTE_DECISION → Esperando que usuario acepte/rechace
 * 4. ACEPTADA → Usuario aceptó, mensaje receptor enviado, compra creada
 * 5. COMPLETADA → Compra finalizada (inventario actualizado, etc.)
 *
 * FLUJOS ALTERNATIVOS:
 * - RECHAZADA → Usuario rechazó la factura
 * - ACEPTADA_PARCIAL → Usuario aceptó con ajuste de IVA
 * - ERROR_HACIENDA → Hacienda rechazó la factura del proveedor
 * - ERROR_PROCESO → Error interno procesando
 */
public enum EstadoCompra {

    // ==================== ESTADOS INICIALES ====================

    /**
     * XML recibido y parseado correctamente
     * Pendiente de validar con Hacienda
     */
    RECIBIDA,

    /**
     * Validada con Hacienda, esperando decisión del usuario
     */
    PENDIENTE_DECISION,

    // ==================== ESTADOS FINALES POSITIVOS ====================

    /**
     * Usuario aceptó la factura
     * Mensaje receptor enviado a Hacienda
     * Compra creada en el sistema
     */
    ACEPTADA,

    /**
     * Usuario aceptó parcialmente (ajuste de IVA)
     * Mensaje receptor enviado
     * Compra creada con monto ajustado
     */
    ACEPTADA_PARCIAL,

    /**
     * Compra completada
     * Inventario actualizado
     * Métricas actualizadas
     */
    COMPLETADA,

    // ==================== ESTADOS FINALES NEGATIVOS ====================

    /**
     * Usuario rechazó la factura del proveedor
     * Mensaje receptor de rechazo enviado
     * NO se crea compra
     */
    RECHAZADA,

    /**
     * Hacienda rechazó la factura del proveedor
     * NO se puede aceptar
     * NO se crea compra
     */
    ERROR_HACIENDA,

    /**
     * Error interno procesando la factura
     * Ej: error parseando XML, error subiendo a S3, etc.
     */
    ERROR_PROCESO,

    // ==================== ESTADOS ESPECIALES ====================

    /**
     * Compra fue anulada posteriormente
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
}