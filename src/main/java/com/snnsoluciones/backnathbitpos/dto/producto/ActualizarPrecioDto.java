package com.snnsoluciones.backnathbitpos.dto.producto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarPrecioDto {
    
    @NotNull(message = "El precio de venta es requerido")
    @DecimalMin(value = "0.00", message = "El precio debe ser mayor o igual a 0")
    @Digits(integer = 13, fraction = 5, message = "Formato de precio inválido")
    private BigDecimal precioVenta;
    
    // Opcional: por si queremos actualizar también si aplica servicio
    private Boolean aplicaServicio;
    
    // Opcional: por si queremos actualizar el tipo de impuesto
    private String codigoTarifaIVA;
}