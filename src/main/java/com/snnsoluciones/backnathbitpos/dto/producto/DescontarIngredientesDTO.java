package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
public class DescontarIngredientesDTO {
    @NotNull(message = "La empresa es requerida")
    private Long empresaId;
    
    @NotNull(message = "El producto es requerido")
    private Long productoId;
    
    @NotNull(message = "La sucursal es requerida")
    private Long sucursalId;
    
    @NotNull(message = "La cantidad es requerida")
    @Positive(message = "La cantidad debe ser mayor a cero")
    private BigDecimal cantidad;
}