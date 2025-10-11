package com.snnsoluciones.backnathbitpos.dto.facturainterna;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedioPagoInternoRequest {
    
    @NotBlank(message = "El tipo de pago es requerido")
    @Pattern(regexp = "EFECTIVO|TARJETA|SINPE|TRANSFERENCIA|CHEQUE", 
             message = "Tipo de pago inválido")
    private String tipoPago;
    
    @NotNull(message = "El monto es requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal monto;
    
    private String referencia; // Número de voucher, código SINPE, etc.
    private String banco; // Para tarjetas o transferencias
    private String numeroAutorizacion;
    private String descripcionPago;

    private Long plataformaDigitalId;
}