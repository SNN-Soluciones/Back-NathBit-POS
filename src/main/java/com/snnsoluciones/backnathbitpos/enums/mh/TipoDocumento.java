package com.snnsoluciones.backnathbitpos.enums.mh;

import java.util.Arrays;
import java.util.Optional;

public enum TipoDocumento {
    FACTURA_ELECTRONICA("01", "Factura Electrónica", true),
    NOTA_DEBITO("02", "Nota de Débito", true),
    NOTA_CREDITO("03", "Nota de Crédito", true),
    TIQUETE_ELECTRONICO("04", "Tiquete Electrónico", true),
    FACTURA_COMPRA("08", "Factura de Compra", true),
    FACTURA_EXPORTACION("09", "Factura de Exportación", true),
    RECIBO_PAGO("10", "Recibo de Pago", true),
    // Documentos internos (no van a Hacienda)
    TIQUETE_INTERNO("TI", "Tiquete Interno", false),
    FACTURA_INTERNA("FI", "Factura Interna", false),
    PROFORMA("PF", "Proforma", false),
    ORDEN_PEDIDO("OP", "Orden de Pedido", false),
    MENSAJE_RECEPTOR("MR", "Mensaje de Receptor", false);

    private final String codigo;
    private final String descripcion;
    private final boolean electronico;

    TipoDocumento(String codigo, String descripcion, boolean electronico) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.electronico = electronico;
    }

    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }
    public boolean isElectronico() { return electronico; }

    Optional<?> fromCodigoOptional(String codigo) {
        if (codigo == null) return Optional.empty();

        return Arrays.stream(values())
            .filter(tipo -> tipo.codigo.equals(codigo))
            .findFirst();
    }

    public static TipoDocumento fromCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        for(TipoDocumento tipo : values()) {
            if(tipo.codigo.equals(codigo)) {
                return tipo;
            }
        }
        return null;
    }
}