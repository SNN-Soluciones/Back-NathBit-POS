package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

@Data
public class InventarioInicialDTO {
    @NotNull(message = "El producto es requerido")
    private Long productoId;
    
    @NotNull(message = "La sucursal es requerida")
    private Long sucursalId;
    
    @NotNull(message = "La cantidad mínima es requerida")
    @PositiveOrZero(message = "La cantidad mínima debe ser mayor o igual a cero")
    private BigDecimal cantidadMinima;
}