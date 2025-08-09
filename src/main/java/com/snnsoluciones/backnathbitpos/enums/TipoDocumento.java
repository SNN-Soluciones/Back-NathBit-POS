package com.snnsoluciones.backnathbitpos.enums;

public enum TipoDocumento {
    // Documentos electrónicos
    FACTURA_ELECTRONICA("01", "Factura Electrónica", true),
    NOTA_DEBITO("02", "Nota de Débito Electrónica", true),
    NOTA_CREDITO("03", "Nota de Crédito Electrónica", true),
    TIQUETE_ELECTRONICO("04", "Tiquete Electrónico", true),
    CONFIRMACION_ACEPTACION("05", "Confirmación de Aceptación", true),
    CONFIRMACION_ACEPTACION_PARCIAL("06", "Confirmación de Aceptación Parcial", true),
    CONFIRMACION_RECHAZO("07", "Confirmación de Rechazo", true),
    FACTURA_COMPRA("08", "Factura Electrónica de Compra", true),
    FACTURA_EXPORTACION("09", "Factura Electrónica de Exportación", true),
    RECIBO_PAGO("10", "Recibo Electrónico de Pago", true),
    
    // Documentos internos
    TIQUETE_INTERNO("TI", "Tiquete Interno", false),
    FACTURA_INTERNA("FI", "Factura Interna", false),
    PROFORMA("PF", "Proforma", false),
    ORDEN_PEDIDO("OP", "Orden de Pedido", false);
    
    private final String codigo;
    private final String descripcion;
    private final boolean esElectronico;
    
    TipoDocumento(String codigo, String descripcion, boolean esElectronico) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.esElectronico = esElectronico;
    }
    
    public String getCodigo() {
        return codigo;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public boolean isEsElectronico() {
        return esElectronico;
    }
    
    public static TipoDocumento porCodigo(String codigo) {
        for (TipoDocumento tipo : values()) {
            if (tipo.codigo.equals(codigo)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Código de documento no válido: " + codigo);
    }
}