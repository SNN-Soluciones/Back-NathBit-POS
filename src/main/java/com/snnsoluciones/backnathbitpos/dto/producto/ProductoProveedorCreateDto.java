package com.snnsoluciones.backnathbitpos.dto.producto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoProveedorCreateDto {
    
    @NotNull(message = "El producto es requerido")
    private Long productoId;
    
    @NotNull(message = "El proveedor es requerido")
    private Long proveedorId;
    
    @NotBlank(message = "El código del proveedor es requerido")
    @Size(max = 50, message = "El código no puede exceder 50 caracteres")
    private String codigoProveedor;
    
    @Size(max = 200, message = "La descripción no puede exceder 200 caracteres")
    private String descripcionProveedor;
    
    @Size(max = 20, message = "La unidad de compra no puede exceder 20 caracteres")
    private String unidadCompra;
    
    @Positive(message = "El factor de conversión debe ser positivo")
    private Integer factorConversion;
    
    @Positive(message = "El precio debe ser positivo")
    private BigDecimal precioCompra;
    
    private String observaciones;
}
