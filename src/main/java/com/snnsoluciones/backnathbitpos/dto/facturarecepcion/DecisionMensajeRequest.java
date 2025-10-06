package com.snnsoluciones.backnathbitpos.dto.facturarecepcion;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DecisionMensajeRequest {
    
    @NotNull(message = "Debe indicar la decisión")
    private TipoDecision decision;
    
    private String justificacion; // Obligatorio si es RECHAZAR
    
    private BigDecimal montoAceptado; // Obligatorio si es PARCIAL
    
    public enum TipoDecision {
        ACEPTAR,
        PARCIAL,
        RECHAZAR
    }
}