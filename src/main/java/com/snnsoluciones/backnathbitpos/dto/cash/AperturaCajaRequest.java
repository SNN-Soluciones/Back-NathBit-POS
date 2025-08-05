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
@Schema(description = "Request para apertura de caja")
public class AperturaCajaRequest {
    
    @NotNull(message = "El ID de la caja es obligatorio")
    private UUID cajaId;
    
    @NotNull(message = "El monto inicial es obligatorio")
    @DecimalMin(value = "0.0", message = "El monto inicial debe ser mayor o igual a 0")
    private BigDecimal montoInicial;
    
    @NotNull(message = "Las denominaciones son obligatorias")
    @Schema(description = "Detalle de billetes y monedas")
    private DenominacionesDTO denominaciones;
    
    @Schema(description = "Observaciones opcionales")
    private String observaciones;
}