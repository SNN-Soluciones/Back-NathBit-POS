package com.snnsoluciones.backnathbitpos.enums.mh;

public enum ProveedorSistema {
    SNN_SOLUCIONES("114970286", "SNN Soluciones", "info@snnsoluciones.com");
    
    private final String identificacion;
    private final String nombre;
    private final String email;
    
    ProveedorSistema(String identificacion, String nombre, String email) {
        this.identificacion = identificacion;
        this.nombre = nombre;
        this.email = email;
    }
    
    public String getIdentificacion() { return identificacion; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
}