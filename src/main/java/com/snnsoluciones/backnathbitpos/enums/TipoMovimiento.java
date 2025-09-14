package com.snnsoluciones.backnathbitpos.enums;

/**
 * Enum para tipos de movimiento de inventario
 */
public enum TipoMovimiento {
    // Entradas
    ENTRADA_COMPRA("Entrada por compra"),
    ENTRADA_DEVOLUCION("Entrada por devolución de cliente"),
    ENTRADA_AJUSTE("Entrada por ajuste de inventario"),
    ENTRADA_TRANSFERENCIA("Entrada por transferencia entre sucursales"),
    ENTRADA_PRODUCCION("Entrada por producción"),
    ENTRADA_INICIAL("Inventario inicial"),
    
    // Salidas
    SALIDA_VENTA("Salida por venta"),
    SALIDA_DEVOLUCION("Salida por devolución a proveedor"),
    SALIDA_AJUSTE("Salida por ajuste de inventario"),
    SALIDA_TRANSFERENCIA("Salida por transferencia entre sucursales"),
    SALIDA_MERMA("Salida por merma o pérdida"),
    SALIDA_ANULACION("Salida por anulación de compra"),
    SALIDA_CONSUMO("Salida por consumo interno");
    
    private final String descripcion;
    
    TipoMovimiento(String descripcion) {
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
}