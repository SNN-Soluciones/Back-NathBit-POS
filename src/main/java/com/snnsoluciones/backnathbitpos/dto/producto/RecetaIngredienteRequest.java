// RecetaIngredienteRequest.java - Para crear/actualizar recetas
package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class RecetaIngredienteRequest {
    
    @NotNull(message = "El ingrediente es requerido")
    private Long ingredienteId;
    
    @NotNull(message = "La cantidad es requerida")
    @DecimalMin(value = "0.0", inclusive = false, message = "La cantidad debe ser mayor a 0")
    private BigDecimal cantidad;
    
    @NotBlank(message = "La unidad de medida es requerida")
    private String unidadMedida;
    
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal factorConversion = BigDecimal.ONE;
    
    private String notasPreparacion;
}