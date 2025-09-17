package com.snnsoluciones.backnathbitpos.dto.pago;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
public class RegistrarPagoDTO {
    @NotNull
    private Long cuentaPorCobrarId;
    
    @NotNull
    @Positive
    private BigDecimal monto;
    
    @NotNull
    private String medioPago; // Código del enum MedioPago
    
    private String referencia; // Para transferencias, cheques
    private String observaciones;
}