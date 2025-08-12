package com.snnsoluciones.backnathbitpos.enums.mh;

import java.util.Arrays;
import java.util.Optional;

public enum UnidadMedida {
    UNIDAD("Unid", "Unidad"),
    SERVICIOS_PROFESIONALES("Sp", "Servicios profesionales"),
    KILOGRAMO("kg", "Kilogramo"),
    GRAMO("g", "Gramo"),
    LITRO("L", "Litro"),
    METRO("m", "Metro"),
    METRO_CUADRADO("m²", "Metro cuadrado"),
    METRO_CUBICO("m³", "Metro cúbico"),
    KILOWATT_HORA("kWh", "Kilowatt hora"),
    DIA("d", "Día"),
    HORA("h", "Hora"),
    OTROS("Otros", "Otros");
    
    private final String codigo;
    private final String descripcion;

    UnidadMedida(String codigo, String descripcion) {
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