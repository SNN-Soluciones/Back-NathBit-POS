package com.snnsoluciones.backnathbitpos.enums;

/**
 * Tipos de movimientos de caja
 * Incluye entradas, salidas, vales y ajustes
 */
public enum TipoMovimientoCaja {
    // ===== ENTRADAS =====
    ENTRADA_ADICIONAL("Entrada adicional de efectivo"),
    ENTRADA_EFECTIVO("Entrada de efectivo"), // 🆕 Para futuro

    // ===== SALIDAS =====
    SALIDA_VALE("Vale de caja"),
    SALIDA_DEPOSITO("Depósito bancario"),
    SALIDA_ARQUEO("Arqueo de caja"), // 🆕 NUEVO
    SALIDA_PAGO_PROVEEDOR("Pago a proveedor"), // 🆕 NUEVO
    SALIDA_OTROS("Otros gastos"), // 🆕 NUEVO

    // ===== AJUSTES =====
    AJUSTE_POSITIVO("Ajuste por sobrante"),
    AJUSTE_NEGATIVO("Ajuste por faltante");

    private final String descripcion;

    TipoMovimientoCaja(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Determina si el movimiento es una entrada
     */
    public boolean esEntrada() {
        return this.name().startsWith("ENTRADA_");
    }

    /**
     * Determina si el movimiento es una salida
     */
    public boolean esSalida() {
        return this.name().startsWith("SALIDA_");
    }

    /**
     * Determina si el movimiento es un ajuste
     */
    public boolean esAjuste() {
        return this.name().startsWith("AJUSTE_");
    }

    /**
     * Requiere autorización de supervisor
     */
    public boolean requiereAutorizacion() {
        return this == SALIDA_VALE ||
            this == SALIDA_ARQUEO ||
            this == SALIDA_PAGO_PROVEEDOR ||
            this == SALIDA_OTROS;
    }
}