package com.snnsoluciones.backnathbitpos.enums.mh;

import java.util.Arrays;
import java.util.Optional;

public enum CondicionVenta {
    CONTADO("01", "Contado"),
    CREDITO("02", "Crédito"),
    CONSIGNACION("03", "Consignación"),
    APARTADO("04", "Apartado"),
    ARRENDAMIENTO_OPCION_COMPRA("05", "Arrendamiento con opción de compra"),
    ARRENDAMIENTO_FINANCIERO("06", "Arrendamiento en función financiera"),
    COBRO_FAVOR_TERCERO("07", "Cobro a favor de un tercero"),
    SERVICIOS_PRESTADOS_ESTADO("08", "Servicios prestados al Estado"),
    PAGO_SERVICIOS_ESTADO("09", "Pago de servicios prestado al Estado"),
    VENTA_CREDITO_IVA_90_DIAS("10", "Venta a crédito en IVA hasta 90 días"),
    PAGO_VENTA_CREDITO_IVA("11", "Pago de venta a crédito en IVA hasta 90 días"),
    VENTA_MERCANCIA_NO_NACIONALIZADA("12", "Venta Mercancía No Nacionalizada"),
    VENTA_BIENES_USADOS("13", "Venta Bienes Usados No Contribuyente"),
    ARRENDAMIENTO_OPERATIVO("14", "Arrendamiento Operativo"),
    ARRENDAMIENTO_FINANCIERO_V2("15", "Arrendamiento Financiero"),
    OTROS("99", "Otros");
    
    private final String codigo;
    private final String descripcion;
    
    CondicionVenta(String codigo, String descripcion) {
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

    public static CondicionVenta fromCodigo(String codigo) {
        return Arrays.stream(values())
            .filter(cv -> cv.getCodigo().equals(codigo))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Código de condición venta no válido: " + codigo));
    }
}