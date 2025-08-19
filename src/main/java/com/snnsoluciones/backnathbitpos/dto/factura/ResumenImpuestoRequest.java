package com.snnsoluciones.backnathbitpos.dto.factura;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenImpuestoRequest {
    
    @NotBlank
    @Size(min = 2, max = 2)
    private String codigoImpuesto;
    
    private String codigoTarifaIVA;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal totalMontoImpuesto;
    
    private BigDecimal totalBaseImponible;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal totalMontoExoneracion;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal totalImpuestoNeto;
    
    @NotNull
    @Min(value = 1)
    private Integer cantidadLineas;
}