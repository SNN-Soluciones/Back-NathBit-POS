// ExoneracionRequest.java - NUEVO
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
public class ExoneracionRequest {
    
    @NotBlank
    @Size(min = 2, max = 2)
    private String tipoDocumentoExoneracion;
    
    @NotBlank
    @Size(min = 3, max = 40)
    private String numeroDocumentoExoneracion;
    
    @NotBlank
    @Size(min = 2, max = 2)
    private String nombreInstitucion;
    
    @NotBlank
    private String fechaEmisionExoneracion;
    
    @NotNull
    @DecimalMin(value = "0.00")
    @DecimalMax(value = "100.00")
    private BigDecimal tarifaExonerada;
}
