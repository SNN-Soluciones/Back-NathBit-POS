package com.snnsoluciones.backnathbitpos.enums;

public enum TipoAutenticacionHacienda {
    LLAVE_CRIPTOGRAFICA("Llave Criptográfica ATV"),
    FIRMA_DIGITAL("Firma Digital (.p12)");
    
    private final String descripcion;
    
    TipoAutenticacionHacienda(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
}