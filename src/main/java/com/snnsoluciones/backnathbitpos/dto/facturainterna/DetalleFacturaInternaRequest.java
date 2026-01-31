package com.snnsoluciones.backnathbitpos.dto.facturainterna;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleFacturaInternaRequest {
    
    @NotNull(message = "El ID del producto es requerido")
    private Long productoId;
    
    @NotNull(message = "La cantidad es requerida")
    @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a 0")
    private BigDecimal cantidad;
    
    @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo")
    private BigDecimal descuento;

    private BigDecimal precioUnitario;
    
    private String notas;

    private BigDecimal subtotal;

    private Long ordenItemId; // ✅ AGREGAR esto
}