package com.snnsoluciones.backnathbitpos.enums;

public enum CondicionVenta {
    CONTADO("01", "Contado"),
    CREDITO("02", "Crédito"),
    CONSIGNACION("03", "Consignación"),
    APARTADO("04", "Apartado"),
    ARRENDAMIENTO_OPCION_COMPRA("05", "Arrendamiento con opción de compra"),
    ARRENDAMIENTO_FUNCION_FINANCIERA("06", "Arrendamiento en función financiera"),
    COBRO_FAVOR_TERCERO("07", "Cobro a favor de un tercero"),
    SERVICIOS_PRESTADOS_ESTADO("08", "Servicios prestados al Estado a crédito"),
    PAGO_SERVICIO_ESTADO("09", "Pago de servicios prestados al Estado"),
    VENTA_CREDITO_IVA("10", "Venta a crédito IVA a 90 días"),
    OTROS("99", "Otros");
    
    private final String codigo;
    private final String descripcion;
    
    CondicionVenta(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }
    
    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }
}