package com.snnsoluciones.backnathbitpos.enums.mh;

import java.util.Arrays;
import java.util.Optional;

public enum TipoTransaccion {
    VENTA_NORMAL("01", "Venta Normal de Bienes y Servicios"),
    AUTOCONSUMO_EXENTO("02", "Mercancía de Autoconsumo exento"),
    AUTOCONSUMO_GRAVADO("03", "Mercancía de Autoconsumo gravado"),
    BIENES_CAPITAL("08", "Bienes de Capital");

    private final String codigo;
    private final String descripcion;

    TipoTransaccion(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }

    /**
     * Obtiene el enum a partir del código de Hacienda
     * @param codigo El código del MH (ej: "01", "02")
     * @return El enum correspondiente
     * @throws IllegalArgumentException si el código no existe
     */
    public static Optional<TipoTransaccion> fromCodigoOptional(String codigo) {
        if (codigo == null) return Optional.empty();

        return Arrays.stream(values())
            .filter(tipo -> tipo.codigo.equals(codigo))
            .findFirst();
    }
}