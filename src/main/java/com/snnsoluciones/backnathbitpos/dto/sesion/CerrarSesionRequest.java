package com.snnsoluciones.backnathbitpos.dto.sesion;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CerrarSesionRequest {
    @NotNull(message = "Monto de cierre es requerido")
    @Min(value = 0, message = "Monto debe ser mayor o igual a 0")
    private BigDecimal montoCierre;
    
    private String observaciones;
}