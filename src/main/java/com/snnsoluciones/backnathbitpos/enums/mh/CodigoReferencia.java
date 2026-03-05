package com.snnsoluciones.backnathbitpos.enums.mh;

import java.util.Arrays;
import java.util.Optional;

public enum CodigoReferencia {
    ANULA_DOCUMENTO("01", "Anula documento de referencia"),
    CORRIGE_TEXTO("02", "Corrige texto documento de referencia"),
    CORRIGE_MONTO("03", "Corrige monto"),
    REFERENCIA_OTRO("04", "Referencia a otro documento"),
    SUSTITUYE_PROVISIONAL("05", "Sustituye comprobante provisional"),
    NOTA_CREDITO_FINANCIERA("09", "Nota de crédito financiera"),   // ← AGREGAR
    NOTA_DEBITO_FINANCIERA("10", "Nota de débito financiera"),     // ← AGREGAR
    OTROS("99", "Otros");
    
    private final String codigo;
    private final String descripcion;
    
    CodigoReferencia(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }
    
    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }

    Optional<?> fromCodigoOptional(String codigo) {
        if (codigo == null) return Optional.empty();

        return Arrays.stream(values())
            .filter(tipo -> tipo.codigo.equals(codigo))
            .findFirst();
    }
}