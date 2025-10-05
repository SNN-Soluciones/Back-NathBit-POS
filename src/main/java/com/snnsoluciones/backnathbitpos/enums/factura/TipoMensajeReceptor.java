package com.snnsoluciones.backnathbitpos.enums.factura;

import lombok.Getter;

@Getter
public enum TipoMensajeReceptor {
    ACEPTADO("1", "Aceptado"),
    ACEPTADO_PARCIAL("2", "Aceptado Parcialmente"),
    RECHAZADO("3", "Rechazado");

    private final String codigo;
    private final String descripcion;

    TipoMensajeReceptor(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public static TipoMensajeReceptor fromCodigo(String codigo) {
        for (TipoMensajeReceptor tipo : values()) {
            if (tipo.codigo.equals(codigo)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Código de mensaje receptor inválido: " + codigo);
    }
}