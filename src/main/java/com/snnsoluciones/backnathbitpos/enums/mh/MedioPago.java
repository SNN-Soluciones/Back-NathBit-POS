package com.snnsoluciones.backnathbitpos.enums.mh;

import java.util.Arrays;
import java.util.Optional;

public enum MedioPago {
    EFECTIVO("01", "Efectivo"),
    TARJETA("02", "Tarjeta"),
    CHEQUE("03", "Cheque"),
    TRANSFERENCIA("04", "Transferencia - depósito bancario"),
    RECAUDADO_TERCEROS("05", "Recaudado por terceros"),
    SINPE_MOVIL("06", "SINPE Móvil"),              // NUEVO v4.4
    PLATAFORMA_DIGITAL("07", "Plataforma Digital"), // NUEVO v4.4
    OTROS("99", "Otros");

    private final String codigo;
    private final String descripcion;

    MedioPago(String codigo, String descripcion) {
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