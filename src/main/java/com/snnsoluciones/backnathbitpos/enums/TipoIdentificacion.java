// Archivo: src/main/java/com/snnsoluciones/backnathbitpos/enums/TipoIdentificacion.java

package com.snnsoluciones.backnathbitpos.enums;

public enum TipoIdentificacion {
    CEDULA_FISICA("01", "Cédula Física"),
    CEDULA_JURIDICA("02", "Cédula Jurídica"),
    DIMEX("03", "DIMEX"),
    NITE("04", "NITE"),
    EXTRANJERO("05", "Extranjero");
    
    private final String codigo;
    private final String descripcion;
    
    TipoIdentificacion(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }
    
    public String getCodigo() {
        return codigo;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
}