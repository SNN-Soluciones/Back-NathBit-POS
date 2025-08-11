package com.snnsoluciones.backnathbitpos.dto.producto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para crear/actualizar
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TarifaIVACreateDto {
    @NotBlank(message = "El código de Hacienda es requerido")
    @Size(min = 2, max = 2, message = "El código debe tener 2 caracteres")
    private String codigoHacienda;
    
    @NotBlank(message = "La descripción es requerida")
    @Size(max = 100, message = "La descripción no puede exceder 100 caracteres")
    private String descripcion;
    
    @NotNull(message = "El porcentaje es requerido")
    @DecimalMin(value = "0.00", message = "El porcentaje no puede ser negativo")
    @DecimalMax(value = "100.00", message = "El porcentaje no puede exceder 100")
    private BigDecimal porcentaje;
}