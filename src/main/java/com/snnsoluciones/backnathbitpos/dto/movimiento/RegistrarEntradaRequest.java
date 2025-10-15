package com.snnsoluciones.backnathbitpos.dto.movimiento;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para registrar una entrada de efectivo
 * Se usará cuando el frontend implemente la funcionalidad
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarEntradaRequest {
    
    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal monto;
    
    @NotBlank(message = "El concepto es obligatorio")
    private String concepto;
    
    @NotBlank(message = "El usuario que autoriza es obligatorio")
    private String usuarioAutoriza;
    
    /**
     * Observaciones adicionales (opcional)
     */
    private String observaciones;
}