package com.snnsoluciones.backnathbitpos.dto.orden;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record AgregarItemRequest(
    @NotNull(message = "Producto ID es requerido")
    Long productoId,
    
    @NotNull(message = "Cantidad es requerida")
    @Min(value = 1, message = "Cantidad debe ser al menos 1")
    BigDecimal cantidad,
    
    String notas,
    
    @Valid
    List<OpcionCompuestaRequest> opciones
) {}
