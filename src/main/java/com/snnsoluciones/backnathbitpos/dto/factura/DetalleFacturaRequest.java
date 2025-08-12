package com.snnsoluciones.backnathbitpos.dto.factura;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleFacturaRequest {
    
    @NotNull(message = "El producto es requerido")
    private Long productoId;
    
    @NotNull(message = "La cantidad es requerida")
    @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor a 0")
    private BigDecimal cantidad;
    
    @NotNull(message = "El precio unitario es requerido")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    private BigDecimal precioUnitario;
    
    // Descuento por línea (opcional)
    private BigDecimal descuento = BigDecimal.ZERO;
    
    // Para override de descripción si es necesario
    private String descripcionPersonalizada;
}