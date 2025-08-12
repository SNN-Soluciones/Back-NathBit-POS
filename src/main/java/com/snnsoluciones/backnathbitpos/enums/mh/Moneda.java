package com.snnsoluciones.backnathbitpos.enums.mh;

import java.math.BigDecimal;

public enum Moneda {
    CRC("CRC", "Colón costarricense", "₡", true),
    USD("USD", "Dólar estadounidense", "$", false),
    EUR("EUR", "Euro", "€", false);
    
    private final String codigo;
    private final String nombre;
    private final String simbolo;
    private final boolean esMonedaLocal;
    
    Moneda(String codigo, String nombre, String simbolo, boolean esMonedaLocal) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.simbolo = simbolo;
        this.esMonedaLocal = esMonedaLocal;
    }
    
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getSimbolo() { return simbolo; }
    public boolean isMonedaLocal() { return esMonedaLocal; }
    
    /**
     * Obtiene el tipo de cambio según las reglas de Hacienda
     * @return 1 para CRC, null para otras (se debe obtener del BCCR)
     */
    public BigDecimal getTipoCambioDefault() {
        return esMonedaLocal ? BigDecimal.ONE : null;
    }
    
    /**
     * Valida si necesita tipo de cambio del BCCR
     */
    public boolean requiereTipoCambioBCCR() {
        return !esMonedaLocal;
    }
    
    /**
     * Obtiene la moneda desde el código
     */
    public static Moneda fromCodigo(String codigo) {
        if (codigo == null) {
            throw new IllegalArgumentException("El código de moneda no puede ser null");
        }
        
        for (Moneda moneda : values()) {
            if (moneda.codigo.equals(codigo)) {
                return moneda;
            }
        }
        throw new IllegalArgumentException("Código de moneda no válido: " + codigo);
    }
    
    /**
     * Formato para mostrar en UI
     */
    public String getDisplayName() {
        return simbolo + " - " + nombre + " (" + codigo + ")";
    }
}