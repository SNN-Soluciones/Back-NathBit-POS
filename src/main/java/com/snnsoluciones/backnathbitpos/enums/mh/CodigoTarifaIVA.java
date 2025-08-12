package com.snnsoluciones.backnathbitpos.enums.mh;

import java.math.BigDecimal;

public enum CodigoTarifaIVA {
    TARIFA_0_EXENTO("01", "Tarifa 0% (Artículo 32, num 1, RLIVA)", BigDecimal.ZERO),
    TARIFA_REDUCIDA_1("02", "Tarifa reducida 1%", new BigDecimal("1")),
    TARIFA_REDUCIDA_2("03", "Tarifa reducida 2%", new BigDecimal("2")),
    TARIFA_REDUCIDA_4("04", "Tarifa reducida 4%", new BigDecimal("4")),
    TRANSITORIO_0("05", "Transitorio 0%", BigDecimal.ZERO),
    TRANSITORIO_4("06", "Transitorio 4%", new BigDecimal("4")),
    TARIFA_TRANSITORIA_8("07", "Tarifa transitoria 8%", new BigDecimal("8")),
    TARIFA_GENERAL_13("08", "Tarifa general 13%", new BigDecimal("13")),
    TARIFA_REDUCIDA_0_5("09", "Tarifa reducida 0.5%", new BigDecimal("0.5")),
    TARIFA_EXENTA("10", "Tarifa Exenta", BigDecimal.ZERO),
    TARIFA_0_SIN_CREDITO("11", "Tarifa 0% sin derecho a crédito", BigDecimal.ZERO);

    private final String codigo;
    private final String descripcion;
    private final BigDecimal porcentaje;

    CodigoTarifaIVA(String codigo, String descripcion, BigDecimal porcentaje) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.porcentaje = porcentaje;
    }

    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }
    public BigDecimal getPorcentaje() { return porcentaje; }

    /**
     * Indica si la tarifa otorga derecho a crédito fiscal
     */
    public boolean otorgaDerechoCredito() {
        return !this.equals(TARIFA_0_SIN_CREDITO);
    }

    /**
     * Indica si es una tarifa exenta o con 0%
     */
    public boolean esExenta() {
        return porcentaje.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Indica si es una tarifa transitoria
     */
    public boolean esTransitoria() {
        return this == TRANSITORIO_0 || this == TRANSITORIO_4 || this == TARIFA_TRANSITORIA_8;
    }

    public static CodigoTarifaIVA fromCodigo(String codigo) {
        if (codigo == null) {
            throw new IllegalArgumentException("El código no puede ser null");
        }

        for (CodigoTarifaIVA tarifa : values()) {
            if (tarifa.codigo.equals(codigo)) {
                return tarifa;
            }
        }
        throw new IllegalArgumentException("Código de tarifa IVA no válido: " + codigo);
    }
}