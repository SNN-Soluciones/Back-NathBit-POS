package com.snnsoluciones.backnathbitpos.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoDocumentoExoneracion {
    ORDEN_COMPRA("01", "Orden de Compra"),
    EXONERACION("02", "Exoneración"),
    AUTORIZACION("03", "Autorización"),
    FRANQUICIA("04", "Franquicia"),
    OTROS("99", "Otros");
    
    private final String codigo;
    private final String descripcion;
    
    public static TipoDocumentoExoneracion fromCodigo(String codigo) {
        for (TipoDocumentoExoneracion tipo : values()) {
            if (tipo.getCodigo().equals(codigo)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Código de tipo documento exoneración no válido: " + codigo);
    }
}