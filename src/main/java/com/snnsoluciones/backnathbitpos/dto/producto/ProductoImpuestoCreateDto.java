package com.snnsoluciones.backnathbitpos.dto.producto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para crear/actualizar impuesto en producto
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoImpuestoCreateDto {
    @NotNull(message = "El tipo de impuesto es requerido")
    private Long tipoImpuestoId;
    
    private Long tarifaIvaId; // Solo si es IVA
    
    @NotNull(message = "El porcentaje es requerido")
    @DecimalMin(value = "0.00", message = "El porcentaje no puede ser negativo")
    @DecimalMax(value = "100.00", message = "El porcentaje no puede exceder 100")
    private BigDecimal porcentaje;
}