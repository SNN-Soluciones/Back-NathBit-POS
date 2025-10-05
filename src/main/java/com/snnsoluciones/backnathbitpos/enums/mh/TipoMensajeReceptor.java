package com.snnsoluciones.backnathbitpos.enums.mh;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoMensajeReceptor {
    
    ACEPTA("05", "Aceptación", "Confirmación de aceptación total del comprobante"),
    ACEPTA_PARCIAL("06", "Aceptación Parcial", "Aceptación parcial con ajustes en montos"),
    RECHAZA("07", "Rechazo", "Rechazo total del comprobante");
    
    private final String codigo;
    private final String nombre;
    private final String descripcion;
    
    public static TipoMensajeReceptor fromCodigo(String codigo) {
        for (TipoMensajeReceptor tipo : values()) {
            if (tipo.getCodigo().equals(codigo)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Código de mensaje receptor inválido: " + codigo);
    }
}