package com.snnsoluciones.backnathbitpos.dto.movimiento;

import com.snnsoluciones.backnathbitpos.enums.TipoMovimientoCaja;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para registrar una salida de efectivo
 * Soporta: ARQUEO, PAGO_PROVEEDOR, OTROS
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarSalidaRequest {
    
    @NotNull(message = "El tipo de salida es obligatorio")
    private TipoMovimientoCaja tipoSalida; // SALIDA_ARQUEO, SALIDA_PAGO_PROVEEDOR, SALIDA_OTROS
    
    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal monto;
    
    @NotBlank(message = "El usuario que autoriza es obligatorio")
    private String usuarioAutoriza;
    
    // ===== CAMPOS OPCIONALES SEGÚN EL TIPO =====
    
    /**
     * Requerido para SALIDA_PAGO_PROVEEDOR
     * Nombre del proveedor al que se le paga
     */
    private String nombreProveedor;
    
    /**
     * Requerido para SALIDA_OTROS
     * Motivo o descripción del gasto
     */
    private String motivo;
    
    /**
     * Observaciones adicionales (opcional para todos)
     */
    private String observaciones;
}