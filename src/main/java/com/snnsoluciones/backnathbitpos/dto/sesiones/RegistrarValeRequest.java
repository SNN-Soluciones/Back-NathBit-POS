package com.snnsoluciones.backnathbitpos.dto.sesiones;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegistrarValeRequest {
    @NotNull(message = "El monto es requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal monto;
    
    @NotBlank(message = "El concepto es requerido")
    @Size(max = 200, message = "El concepto no puede exceder 200 caracteres")
    private String concepto;
    
    private String observaciones;
}