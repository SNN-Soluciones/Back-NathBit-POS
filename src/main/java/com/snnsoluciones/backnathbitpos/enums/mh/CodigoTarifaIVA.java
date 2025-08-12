package com.snnsoluciones.backnathbitpos.enums.mh;

import java.util.Arrays;
import java.util.Optional;

public enum CodigoTarifaIVA {
    EXENTO("01", "Tarifa 0% (Exento)", 0),
    TARIFA_1("02", "Tarifa reducida 1%", 1),
    TARIFA_2("03", "Tarifa reducida 2%", 2),
    TARIFA_4("04", "Tarifa reducida 4%", 4),
    TARIFA_8("08", "Tarifa reducida 8%", 8),
    TARIFA_13("07", "Tarifa general 13%", 13),
    EXENTO_COMPRAS("10", "Exento (Compras autorizadas)", 0),
    NO_SUJETO("11", "No sujeto", 0);
    
    private final String codigo;
    private final String descripcion;
    private final int porcentaje;

    CodigoTarifaIVA(String codigo, String descripcion, int porcentaje) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.porcentaje = porcentaje;
    }

    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }
    public int getPorcentaje() { return porcentaje; }

    Optional<?> fromCodigoOptional(String codigo) {
        if (codigo == null) return Optional.empty();

        return Arrays.stream(values())
            .filter(tipo -> tipo.codigo.equals(codigo))
            .findFirst();
    }
}