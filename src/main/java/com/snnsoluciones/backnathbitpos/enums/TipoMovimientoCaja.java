package com.snnsoluciones.backnathbitpos.enums;

public enum TipoMovimientoCaja {
    ENTRADA_ADICIONAL("Entrada adicional de efectivo"),
    SALIDA_VALE("Vale de caja"),
    SALIDA_DEPOSITO("Depósito bancario"),
    AJUSTE_POSITIVO("Ajuste por sobrante"),
    AJUSTE_NEGATIVO("Ajuste por faltante");
    
    private final String descripcion;
    
    TipoMovimientoCaja(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
}