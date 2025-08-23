package com.snnsoluciones.backnathbitpos.dto.factura;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImpuestoLineaRequest {
    
    @NotBlank
    @Size(min = 2, max = 2)
    private String codigoImpuesto;
    
    private String codigoTarifaIVA;
    
    @NotNull
    @DecimalMin(value = "0.00")
    @DecimalMax(value = "100.00")
    private BigDecimal tarifa;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal montoImpuesto;
    
    private BigDecimal baseImponible;
    
    @NotNull
    private Boolean tieneExoneracion;
    
    private BigDecimal montoExoneracion;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal impuestoNeto;
    
    @Valid
    private ExoneracionRequest exoneracion;

    // Nuevo soporte v4.4
    private Boolean impuestoAsumidoPorEmisor; // default false
    @DecimalMin("0.00")
    private BigDecimal montoImpuestoAsumido;  // si aplica
}

