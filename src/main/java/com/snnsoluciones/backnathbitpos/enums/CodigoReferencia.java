package com.snnsoluciones.backnathbitpos.enums;

public enum CodigoReferencia {
    ANULA_DOCUMENTO("01", "Anula documento de referencia"),
    CORRIGE_TEXTO("02", "Corrige texto documento de referencia"),
    CORRIGE_MONTO("03", "Corrige monto"),
    REFERENCIA_OTRO("04", "Referencia a otro documento"),
    SUSTITUYE_PROVISIONAL("05", "Sustituye comprobante provisional"),
    OTROS("99", "Otros");
    
    private final String codigo;
    private final String descripcion;
    
    CodigoReferencia(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }
    
    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }
}