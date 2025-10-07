package com.snnsoluciones.backnathbitpos.dto.facturarecepcion;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DecisionMensajeRequest {

    @NotNull(message = "Debe indicar la decisión")
    private TipoDecision decision;

    /**
     * Razón/justificación de la decisión
     * OBLIGATORIO para RECHAZAR y PARCIAL
     * Mínimo 5 caracteres, máximo 160 según Hacienda
     */
    private String razon;

    /**
     * Monto total aceptado (para PARCIAL)
     * Debe ser menor o igual al total de la factura
     */
    private BigDecimal montoAceptado;

    /**
     * Monto de IVA aceptado (para PARCIAL)
     * Debe ser menor o igual al IVA total de la factura
     */
    private BigDecimal montoIvaAceptado;

    public enum TipoDecision {
        ACEPTAR,
        PARCIAL,
        RECHAZAR
    }
}