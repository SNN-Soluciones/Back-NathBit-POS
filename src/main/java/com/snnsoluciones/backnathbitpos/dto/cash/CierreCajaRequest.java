package com.snnsoluciones.backnathbitpos.dto.cash;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para cierre de caja")
public class CierreCajaRequest {
    
    @NotNull(message = "El ID de apertura es obligatorio")
    private UUID aperturaId;
    
    @NotNull(message = "El monto final es obligatorio")
    @DecimalMin(value = "0.0", message = "El monto final debe ser mayor o igual a 0")
    private BigDecimal montoFinal;
    
    @NotNull(message = "Las denominaciones son obligatorias")
    private DenominacionesDTO denominaciones;
    
    @Schema(description = "Observaciones del cierre")
    private String observaciones;
}