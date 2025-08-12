package com.snnsoluciones.backnathbitpos.dto.factura;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaListaResponse {
    
    private Long id;
    private String consecutivo;
    private String tipoDocumento;
    private LocalDateTime fechaEmision;
    private String clienteNombre;
    private BigDecimal total;
    private String estado;
    private String estadoColor; // Para UI: green, red, yellow, gray
    
    // Acciones disponibles
    private boolean puedeImprimir;
    private boolean puedeAnular;
    private boolean puedeReenviar;
}