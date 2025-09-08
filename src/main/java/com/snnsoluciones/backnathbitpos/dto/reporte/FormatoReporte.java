// FormatoReporte enum (si no existe)
package com.snnsoluciones.backnathbitpos.dto.reporte;

public enum FormatoReporte {
    PDF("application/pdf"),
    EXCEL("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    JSON("application/json");
    
    private final String contentType;
    
    FormatoReporte(String contentType) {
        this.contentType = contentType;
    }
    
    public String getContentType() {
        return contentType;
    }
}