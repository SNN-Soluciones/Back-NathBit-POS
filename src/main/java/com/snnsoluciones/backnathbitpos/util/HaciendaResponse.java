package com.snnsoluciones.backnathbitpos.util;

import lombok.Data;

// DTO para respuesta
@Data
public class HaciendaResponse {
    private String clave;
    private String fecha;
    private String indEstado; // procesando, aceptado, rechazado
    private String respuestaXml; // Base64
    
    public boolean isAceptado() {
        return "aceptado".equalsIgnoreCase(indEstado);
    }
    
    public boolean isRechazado() {
        return "rechazado".equalsIgnoreCase(indEstado);
    }
}
