package com.snnsoluciones.backnathbitpos.dto.documento;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Clase Response separada
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerarClaveResponse {
    
    private String clave; // Los 50 dígitos
    private String codigoSeguridad; // Los 8 dígitos generados
    
    // Desglose para debugging
    private String pais; // 506
    private String fecha; // DDMMAAAA
    private String identificacion; // 12 dígitos con padding
    private String consecutivo; // 20 dígitos
    private String situacion; // 1 dígito
    
    // Info adicional
    private LocalDateTime fechaGeneracion;
    private String mensaje;
}