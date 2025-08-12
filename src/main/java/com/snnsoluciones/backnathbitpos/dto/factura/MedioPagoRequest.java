package com.snnsoluciones.backnathbitpos.dto.factura;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedioPagoRequest {
    
    @NotBlank(message = "El medio de pago es requerido")
    @Pattern(regexp = "^(01|02|03|04|05|06|07|99)$", message = "Medio de pago inválido")
    private String medioPago; // 01=Efectivo, 02=Tarjeta, etc.
    
    @NotNull(message = "El monto es requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal monto;
    
    // Para tarjetas, transferencias, etc.
    private String referencia;
    
    // Banco o emisor de tarjeta
    private String banco;
}