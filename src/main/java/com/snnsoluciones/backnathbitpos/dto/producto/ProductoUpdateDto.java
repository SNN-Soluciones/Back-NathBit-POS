package com.snnsoluciones.backnathbitpos.dto.producto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para actualizar producto
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoUpdateDto {
    @NotBlank(message = "El código interno es requerido")
    @Size(max = 20, message = "El código interno no puede exceder 20 caracteres")
    private String codigoInterno;
    
    @Size(max = 30, message = "El código de barras no puede exceder 30 caracteres")
    private String codigoBarras;
    
    @NotBlank(message = "El nombre es requerido")
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    private String nombre;
    
    private String descripcion;
    
    @NotNull(message = "El código CAByS es requerido")
    private Long empresaCabysId;
    
    private Long categoriaId;
    
    @NotNull(message = "La unidad de medida es requerida")
    private Long unidadMedidaId;
    
    @NotNull(message = "La moneda es requerida")
    private Long monedaId;
    
    @NotNull(message = "El precio de venta es requerido")
    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    @Digits(integer = 13, fraction = 5, message = "El precio debe tener máximo 13 enteros y 5 decimales")
    private BigDecimal precioVenta;
    
    @NotNull(message = "Debe indicar si aplica servicio")
    private Boolean aplicaServicio;
    
    @NotNull(message = "Debe indicar si es servicio")
    private Boolean esServicio;
    
    private Boolean activo;
}